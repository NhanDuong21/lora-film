# LoraFilm Frontend

React 19 application cho luồng khách hàng, nhân viên, quản lý rạp và quản trị
viên. Client dùng Vite, React Router, Axios, Vitest và Tailwind CSS.

## Yêu cầu và cài đặt

Sử dụng phiên bản Node.js tương thích với Vite 8 và npm. Từ thư mục `client/`:

```powershell
npm ci
Copy-Item .env.example .env
npm run dev
```

Ứng dụng mặc định chạy tại `http://localhost:5173`. API Gateway cần chạy tại
`http://localhost:8080`; Vite proxy các đường dẫn `/api`, OAuth và `/socket.io`
tới Gateway.

`VITE_API_BASE_URL` nên để trống khi dùng Vite proxy. Chỉ đặt
`VITE_BOOKING_SOCKET_URL` khi cần kết nối Socket.IO trực tiếp. Không bật
`VITE_USE_AUTH_MOCK` cho kiểm thử tích hợp với backend thật.

## Scripts

| Lệnh | Mục đích |
|---|---|
| `npm run dev` | Development server tại cổng 5173 |
| `npm run dev:mobile` | HTTPS development server tại cổng 5174 để test thiết bị |
| `npm test` | Chạy Vitest một lần |
| `npm run test:watch` | Chạy Vitest ở watch mode |
| `npm run lint` | Kiểm tra ESLint toàn client |
| `npm run build` | Tạo production bundle trong `dist/` |
| `npm run preview` | Preview production bundle |

Trước Merge Request, tối thiểu chạy:

```powershell
npm test
npm run lint
npm run build
```

## Cấu trúc source

```text
src/
├── components/  # Layout và component dùng chung
├── contexts/    # Auth và application context
├── features/    # Module nghiệp vụ theo feature
├── routes/      # Composition của route cấp ứng dụng
├── services/    # Client/hạ tầng dùng chung
├── test/        # Test setup
└── utils/       # Formatter và utility thuần
```

Các feature chính gồm auth, catalog, facilities, scheduling, pricing, booking,
payment, promotion, score, notification, concessions và internal staff. Route
được khai báo trong `src/features/**/routes.jsx` rồi ghép tại
`src/routes/AppRoutes.jsx`; không duy trì danh sách route thủ công trong README.

Các khu vực chính:

- `/`: public/customer experience.
- `/employee`: nghiệp vụ tại quầy, soát vé, ca làm và hồ sơ nhân viên.
- `/manager`: vận hành theo rạp của cinema manager.
- `/admin`: quản trị hệ thống theo permission.

## Quy ước tích hợp

- Gọi backend qua shared API client và Gateway; không hard-code URL của từng
  microservice trong component.
- Route admin/employee/manager phải dùng guard hiện có trong `AppRoutes.jsx`.
- Giữ server state trong hook/service của feature; component trình bày không tự
  lặp logic unwrap response hoặc xử lý token.
- Cập nhật test khi thay đổi route, permission, API mapping hoặc trạng thái lỗi.

Tài liệu contract và runbook liên quan nằm trong
[mục lục tài liệu dự án](../docs/README.md).
