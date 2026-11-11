package net.jetztgrad.midi.workshop.push2;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * LibUsbDisplay — drives the Push 2 LCD with plain Java: libusb is called
 * through the Foreign Function &amp; Memory API (Java 22+), no JNI, no libraries.
 *
 * The whole protocol (Ableton "Push 2 MIDI and Display Interface", section 3):
 *   • USB device VID 0x2982 / PID 0x1967, claim interface 0, bulk OUT endpoint 0x01
 *   • per frame: a 16-byte header  FF CC AA 88 00 00 00 00 00 00 00 00 00 00 00 00
 *   • then 160 lines × 2048 bytes: 960 pixels × 2 bytes + 128 filler bytes
 *   • pixel = 16-bit little-endian, blue in bits 15–11, green 10–5, red 4–0
 *   • every line is XOR-ed with the repeating byte pattern E7 F3 E7 FF
 *
 * Needs libusb installed (macOS: {@code brew install libusb}). Override the
 * library path with {@code -Dpush2.libusb=/path/to/libusb-1.0.dylib}.
 * Run the JVM with {@code --enable-native-access=ALL-UNNAMED}.
 */
final class LibUsbDisplay implements Push2.Display.Backend {

    static final short VENDOR_ID  = 0x2982;
    static final short PRODUCT_ID = 0x1967;
    static final int   INTERFACE  = 0;
    static final byte  ENDPOINT   = 0x01;

    static final int WIDTH = 960, HEIGHT = 160;
    static final int LINE_BYTES  = 2048;                     // 1920 pixel bytes + 128 filler
    static final int FRAME_BYTES = HEIGHT * LINE_BYTES;      // 327 680
    static final int CHUNK_BYTES = 16 * 1024;                // bulk transfer size
    static final int TIMEOUT_MS  = 1000;

    static final byte[] HEADER = { (byte) 0xFF, (byte) 0xCC, (byte) 0xAA, (byte) 0x88,
                                   0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    static final byte[] XOR_MASK = { (byte) 0xE7, (byte) 0xF3, (byte) 0xE7, (byte) 0xFF };

    // ── libusb functions, bound via FFM ──────────────────────────────────────
    //   int  libusb_init(libusb_context **ctx)
    //   libusb_device_handle *libusb_open_device_with_vid_pid(libusb_context *, uint16_t vid, uint16_t pid)
    //   int  libusb_claim_interface(libusb_device_handle *, int interface)
    //   int  libusb_bulk_transfer(libusb_device_handle *, unsigned char endpoint,
    //                             unsigned char *data, int length, int *transferred, unsigned int timeout)
    //   int  libusb_release_interface(libusb_device_handle *, int interface)
    //   void libusb_close(libusb_device_handle *)
    //   void libusb_exit(libusb_context *)
    //   const char *libusb_error_name(int code)

    private final MethodHandle init, openDevice, claimInterface, bulkTransfer,
                               releaseInterface, closeDevice, exit, errorName;

    private final Arena arena = Arena.ofShared();            // all native memory lives here
    private final MemorySegment context, handle;
    private final MemorySegment header, frame, transferred;
    private final byte[] lineBuffer = new byte[LINE_BYTES];
    private final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_USHORT_565_RGB);
    private final short[] pixels = ((DataBufferUShort) image.getRaster().getDataBuffer()).getData();

    LibUsbDisplay() {
        Linker linker = Linker.nativeLinker();
        SymbolLookup libusb = SymbolLookup.libraryLookup(findLibUsb(), arena);
        init             = bind(linker, libusb, "libusb_init",                     FunctionDescriptor.of(JAVA_INT, ADDRESS));
        openDevice       = bind(linker, libusb, "libusb_open_device_with_vid_pid", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_SHORT, JAVA_SHORT));
        claimInterface   = bind(linker, libusb, "libusb_claim_interface",          FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        bulkTransfer     = bind(linker, libusb, "libusb_bulk_transfer",            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_BYTE, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
        releaseInterface = bind(linker, libusb, "libusb_release_interface",        FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
        closeDevice      = bind(linker, libusb, "libusb_close",                    FunctionDescriptor.ofVoid(ADDRESS));
        exit             = bind(linker, libusb, "libusb_exit",                     FunctionDescriptor.ofVoid(ADDRESS));
        errorName        = bind(linker, libusb, "libusb_error_name",               FunctionDescriptor.of(ADDRESS, JAVA_INT));

        try {
            MemorySegment contextPtr = arena.allocate(ADDRESS);
            check((int) init.invokeExact(contextPtr), "libusb_init");
            context = contextPtr.get(ADDRESS, 0);

            handle = (MemorySegment) openDevice.invokeExact(context, VENDOR_ID, PRODUCT_ID);
            if (handle.equals(MemorySegment.NULL)) {
                exit.invokeExact(context);
                throw new IllegalStateException("Unable to find Ableton Push 2 display device");
            }
            int rc = (int) claimInterface.invokeExact(handle, INTERFACE);
            if (rc != 0) {
                closeDevice.invokeExact(handle);
                exit.invokeExact(context);
                check(rc, "libusb_claim_interface");
            }
        } catch (RuntimeException | Error e) {
            arena.close();
            throw e;
        } catch (Throwable t) {
            arena.close();
            throw new IllegalStateException(t);
        }
        header = arena.allocate(HEADER.length);
        MemorySegment.copy(HEADER, 0, header, JAVA_BYTE, 0, HEADER.length);
        frame = arena.allocate(FRAME_BYTES);
        transferred = arena.allocate(JAVA_INT);
        for (int i = 2 * WIDTH; i < LINE_BYTES; i++)    // filler bytes: 0 XOR mask, same every line
            lineBuffer[i] = XOR_MASK[i & 3];
    }

    @Override public BufferedImage image() { return image; }

    /** Encodes the image (RGB565 → BGR565, little-endian, XOR) and sends header + frame. */
    @Override
    public void sendFrame() {
        for (int y = 0; y < HEIGHT; y++) {
            encodeLine(pixels, y, lineBuffer);
            MemorySegment.copy(lineBuffer, 0, frame, JAVA_BYTE, (long) y * LINE_BYTES, LINE_BYTES);
        }
        transfer(header, HEADER.length);
        for (int offset = 0; offset < FRAME_BYTES; offset += CHUNK_BYTES) {
            int length = Math.min(CHUNK_BYTES, FRAME_BYTES - offset);
            transfer(frame.asSlice(offset, length), length);
        }
    }

    /**
     * Encodes image line {@code y} (TYPE_USHORT_565_RGB pixels) into the 2048-byte
     * Push line format. Filler bytes 1920–2047 are left untouched (set once).
     */
    static void encodeLine(short[] pixels, int y, byte[] line) {
        for (int x = 0; x < WIDTH; x++) {
            int rgb = pixels[y * WIDTH + x] & 0xFFFF;              // RRRRRGGG GGGBBBBB
            int bgr = (rgb & 0x001F) << 11 | (rgb & 0x07E0) | (rgb >>> 11);
            int i = 2 * x;                                          // little-endian, then XOR
            line[i]     = (byte) (bgr       ^ XOR_MASK[i & 3]);
            line[i + 1] = (byte) (bgr >>> 8 ^ XOR_MASK[(i + 1) & 3]);
        }
    }

    private void transfer(MemorySegment data, int length) {
        try {
            check((int) bulkTransfer.invokeExact(handle, ENDPOINT, data, length, transferred, TIMEOUT_MS),
                  "libusb_bulk_transfer");
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    @Override
    public void close() {
        try {
            int rc = (int) releaseInterface.invokeExact(handle, INTERFACE);
            closeDevice.invokeExact(handle);
            exit.invokeExact(context);
        } catch (Throwable ignored) {
            // closing anyway
        } finally {
            arena.close();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void check(int rc, String function) {
        if (rc >= 0) return;
        String name;
        try {
            name = ((MemorySegment) errorName.invokeExact(rc)).reinterpret(64).getString(0);
        } catch (Throwable t) {
            name = "error " + rc;
        }
        throw new IllegalStateException(function + " failed: " + name);
    }

    private static MethodHandle bind(Linker linker, SymbolLookup lib, String name, FunctionDescriptor fd) {
        return linker.downcallHandle(lib.find(name).orElseThrow(
                () -> new IllegalStateException("libusb has no symbol " + name)), fd);
    }

    /** -Dpush2.libusb wins; else the usual Homebrew / Linux / Windows locations. */
    private static String findLibUsb() {
        String configured = System.getProperty("push2.libusb");
        if (configured != null) return configured;
        List<String> candidates = new ArrayList<>(List.of(
                "/opt/homebrew/lib/libusb-1.0.0.dylib",              // macOS, Apple Silicon Homebrew
                "/usr/local/lib/libusb-1.0.0.dylib"));               // macOS, Intel Homebrew
        for (String path : candidates)
            if (Files.exists(Path.of(path))) return path;
        // Let the OS loader search its default paths (Linux, Windows PATH)
        return System.getProperty("os.name").startsWith("Windows") ? "libusb-1.0.dll" : "libusb-1.0.so.0";
    }
}
