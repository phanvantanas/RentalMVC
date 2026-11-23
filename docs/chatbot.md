# Chatbot tư vấn LensCam

Chatbot có mặt trên trang chủ và các trang khách hàng dùng header chung. Khách chưa đăng nhập vẫn có thể hỏi. Hội thoại giữ tối đa 6 cặp hỏi/đáp trong phiên, không lưu xuống cơ sở dữ liệu.

## Cấu hình máy chủ

- Ưu tiên biến môi trường `GEMINI_API_KEY` và `GEMINI_MODEL` của tiến trình Tomcat.
- Hoặc sao chép `web/WEB-INF/config/chatbot.properties.example` thành `chatbot.properties` rồi điền khóa. Tệp thực đã được Git bỏ qua, nằm trong `WEB-INF` nên không được phục vụ trực tiếp cho trình duyệt.
- Model mặc định: `gemini-3.5-flash-lite`. Có thể đổi bằng cấu hình nếu dự án Google dùng model khác.
- Sau khi thay đổi, build/deploy ứng dụng. Không đặt khóa trong JSP, JavaScript hoặc URL.

## Cách hoạt động

`ChatbotServlet` nhận câu hỏi có CSRF, lấy tối đa 40 sản phẩm đang hoạt động qua `ChatCatalogDao`, gọi `GeminiChatService` rồi trả văn bản. Chỉ dữ liệu danh mục công khai và nội dung hội thoại được gửi sang Google; không lấy hồ sơ, mật khẩu, đơn hàng hay giao dịch của khách.

Giới hạn: 1.200 ký tự/câu hỏi, 20 lượt/phút/địa chỉ IP, một câu đang xử lý mỗi phiên và tối đa 4 yêu cầu AI đồng thời trên mỗi tiến trình ứng dụng. Có timeout và thông báo khi Google trả lỗi hoặc hết hạn mức. Không tự động thử lại để tránh phát sinh lượt gọi trùng.

Trợ lý không có quyền tạo/hủy đơn hoặc xác nhận thanh toán. Giá và lịch trống cuối cùng vẫn được kiểm tra tại bước đặt thuê. Danh mục chỉ lấy tối đa 40 sản phẩm, không phải công cụ tìm kiếm toàn bộ kho.

## Kiểm tra

- `security.ChatbotTest`: JSON, tạo request, xử lý kết quả, CSRF, lịch sử và giới hạn lượt gửi; không gọi API thật.
- `node test/js/chatbot.test.cjs`: giao diện, gửi câu hỏi, hiển thị văn bản an toàn và xử lý lỗi phiên.
- `security.GeminiChatProbe web/WEB-INF/config/chatbot.properties`: kiểm tra trực tiếp với Google bằng một câu hỏi mẫu; có sử dụng hạn mức API.

Tham khảo chính thức: [Gemini generateContent](https://ai.google.dev/api/generate-content), [quản lý khóa API](https://ai.google.dev/gemini-api/docs/api-key).
