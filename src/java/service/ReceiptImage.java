package service;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;

public final class ReceiptImage {

    public static final int MAX_BYTES = 5 * 1024 * 1024;

    public record Validated(byte[] bytes, String contentType) {}

    private ReceiptImage() {}

    public static Validated validate(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("Ảnh chuyển khoản phải nhỏ hơn hoặc bằng 5 MB.");
        }
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Chỉ nhận ảnh PNG hoặc JPG hợp lệ.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!"PNG".equalsIgnoreCase(format)
                        && !"JPEG".equalsIgnoreCase(format)
                        && !"JPG".equalsIgnoreCase(format)) {
                    throw new IllegalArgumentException("Chỉ nhận ảnh PNG hoặc JPG.");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0
                        || height <= 0
                        || width > 6000
                        || height > 6000
                        || (long) width * height > 20000000L) {
                    throw new IllegalArgumentException(
                            "Ảnh quá lớn, vui lòng tải ảnh dưới 20 megapixel.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IllegalArgumentException("Không đọc được ảnh biên lai.");
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                boolean png = "PNG".equalsIgnoreCase(format);
                if (!ImageIO.write(image, png ? "png" : "jpg", output)) {
                    throw new IOException("Cannot encode receipt image");
                }
                if (output.size() > MAX_BYTES) {
                    throw new IllegalArgumentException(
                            "Ảnh sau xử lý vượt quá 5 MB. Vui lòng giảm kích thước ảnh.");
                }
                return new Validated(output.toByteArray(), png ? "image/png" : "image/jpeg");
            } finally {
                reader.dispose();
            }
        } catch (javax.imageio.IIOException ex) {
            throw new IllegalArgumentException("Ảnh bị lỗi hoặc không đầy đủ.");
        }
    }
}
