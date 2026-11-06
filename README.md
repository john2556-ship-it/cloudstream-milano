# Milano – nguồn KKPhim cho CloudStream

Plugin CloudStream đọc phim từ KKPhim (phimapi.com), dùng cùng API với web app Milano Phim.

- **Trang chủ:** Mới cập nhật, Phim bộ, Thuyết minh, Lồng tiếng, Phim lẻ, Hàn Quốc, Trung Quốc, Hoạt hình, TV Shows.
- **Tìm kiếm** theo tên phim.
- **Mỗi tập** có đủ các server (Vietsub / Thuyết minh / Lồng tiếng) để chọn.

## Cài lên GitHub (một lần)

1. Tạo repo mới trên GitHub, ví dụ `milano-cs`, để **Public**.
2. Upload toàn bộ nội dung thư mục này lên nhánh `main`, kể cả thư mục `.github`.
3. Vào tab **Actions** và chờ job **Build** chạy xong (khoảng 3–5 phút, có dấu ✅).
4. Link repo cho CloudStream có dạng:
   `https://raw.githubusercontent.com/<tên-github>/milano-cs/builds/repo.json`

## Thêm vào CloudStream (Android TV)

Vào **Settings → Extensions → Add repository**, dán link ở bước 4, rồi cài **KKPhim**.

## Cập nhật plugin

Sửa code, tăng `version` trong `KKPhim/build.gradle.kts`, rồi đẩy lên GitHub. GitHub tự build lại, CloudStream sẽ báo có bản mới.
