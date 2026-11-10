package vn.edu.vhu.ltdd.a7permission;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.provider.ContactsContract;
import android.database.Cursor;

public class MainActivity extends AppCompatActivity {

    // TODO: thay 2201234567 bằng MSSV của bạn
    private static final String TAG = "A7_2201234567";

    private static final String CHANNEL_ID = "a7_channel";
    private static final int NOTI_ID = 1001;

    private ImageView imgAnh;
    private TextView tvTrangThai;
    private String thongTinAnh = "";

    // ---------------------------------------------------------------
    // 1) Bộ xin quyền CAMERA.
    //    registerForActivityResult phải được gọi TRƯỚC khi Activity ở trạng thái
    //    STARTED, nên ta khai báo ở cấp field (chạy trong constructor).
    // ---------------------------------------------------------------
    private final ActivityResultLauncher<String> xinQuyenCamera = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            duocCap -> {
                Log.d(TAG, "Kết quả xin quyền CAMERA: " + duocCap);
                if (duocCap) {
                    moCamera();
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    // Trạng thái 2: từ chối nhưng còn hỏi lại được
                    hienGiaiThich();
                } else {
                    // Trạng thái 3: đã chọn "Không cho phép" nhiều lần → hệ thống không
                    // hiện hộp thoại nữa, chỉ còn cách vào Cài đặt
                    hienMoCaiDat();
                }
            });

    // 2) Bộ xin quyền POST_NOTIFICATIONS (chỉ cần từ Android 13 / API 33)
    private final ActivityResultLauncher<String> xinQuyenThongBao = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            duocCap -> {
                if (duocCap) {
                    guiThongBao();
                } else {
                    Toast.makeText(this, R.string.noti_denied, Toast.LENGTH_SHORT).show();
                }
            });

    // 3) Bộ mở Camera lấy ảnh xem trước (bitmap thu nhỏ, không cần lưu file)
    private final ActivityResultLauncher<Void> chupAnh = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            (Bitmap bitmap) -> {
                if (bitmap != null) {
                    imgAnh.setImageBitmap(bitmap);

                    // SỬA DÒNG NÀY: Lưu thông tin kích thước vào biến
                    thongTinAnh = getString(R.string.status_photo, bitmap.getWidth(), bitmap.getHeight());

                    // Kích hoạt cập nhật gộp cả quyền và kích thước ảnh
                    capNhatTrangThai();
                } else {
                    Toast.makeText(this, R.string.photo_cancelled, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        imgAnh = findViewById(R.id.imgAnh);
        tvTrangThai = findViewById(R.id.tvTrangThai);
        Button btnChupAnh = findViewById(R.id.btnChupAnh);
        Button btnThongBao = findViewById(R.id.btnThongBao);
        Button btnCaiDat = findViewById(R.id.btnCaiDat);

        taoKenhThongBao();

        btnChupAnh.setOnClickListener(v -> kiemTraRoiChup());
        btnThongBao.setOnClickListener(v -> kiemTraRoiGuiThongBao());
        btnCaiDat.setOnClickListener(v -> moCaiDatUngDung());
        Button btnXinNhieuQuyen = findViewById(R.id.btnXinNhieuQuyen);

        btnXinNhieuQuyen.setOnClickListener(v -> {
            // Kiểm tra nếu là Android 13 (API 33) trở lên thì mới xin quyền thông báo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                xinNhieuQuyen.launch(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.POST_NOTIFICATIONS
                });
            } else {
                // Dưới Android 13 thì chỉ cần xin quyền Camera
                xinNhieuQuyen.launch(new String[]{
                        Manifest.permission.CAMERA
                });
            }
        });
        Button btnDocDanhBa = findViewById(R.id.btnDocDanhBa);

        btnDocDanhBa.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED) {
                docVaHienThiDanhBa();
            } else {
                xinQuyenDanhBa.launch(Manifest.permission.READ_CONTACTS);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        capNhatTrangThai();   // người dùng có thể vừa đổi quyền trong Cài đặt rồi quay lại
    }

    // =====================  LUỒNG XIN QUYỀN CAMERA  =====================

    /** Mẫu chuẩn 3 nhánh mà Google khuyến nghị. */
    private void kiemTraRoiChup() {
        int trangThai = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (trangThai == PackageManager.PERMISSION_GRANTED) {
            // Nhánh 1: đã có quyền → dùng ngay
            moCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // Nhánh 2: đã từng từ chối → giải thích lý do trước khi hỏi lại
            hienGiaiThich();
        } else {
            // Nhánh 3: lần đầu (hoặc đã bị chặn vĩnh viễn) → gọi thẳng hộp thoại hệ thống
            xinQuyenCamera.launch(Manifest.permission.CAMERA);
        }
    }

    private void hienGiaiThich() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.rationale_title)
                .setMessage(R.string.rationale_camera)
                .setPositiveButton(R.string.agree,
                        (d, w) -> xinQuyenCamera.launch(Manifest.permission.CAMERA))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void hienMoCaiDat() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.blocked_title)
                .setMessage(R.string.blocked_camera)
                .setPositiveButton(R.string.open_settings, (d, w) -> moCaiDatUngDung())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void moCamera() {
        chupAnh.launch(null);
    }

    /** Mở đúng trang thông tin ứng dụng của app này trong Cài đặt. */
    private void moCaiDatUngDung() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(i);
    }

    // =====================  LUỒNG THÔNG BÁO  =====================

    private void kiemTraRoiGuiThongBao() {
        // Trước Android 13 không có quyền POST_NOTIFICATIONS → gửi được ngay
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            guiThongBao();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            guiThongBao();
        } else {
            xinQuyenThongBao.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** Từ Android 8 (API 26) mọi thông báo đều phải thuộc về một kênh. */
    private void taoKenhThongBao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel kenh = new NotificationChannel(
                    CHANNEL_ID, getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            kenh.setDescription(getString(R.string.channel_desc));
            getSystemService(NotificationManager.class).createNotificationChannel(kenh);
        }
    }

    private void guiThongBao() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.noti_title))
                .setContentText(getString(R.string.noti_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(this).notify(NOTI_ID, b.build());
        } catch (SecurityException e) {
            // Người dùng vừa tắt quyền ở Cài đặt trong lúc app đang chạy
            Log.w(TAG, "Thiếu quyền gửi thông báo", e);
        }
    }
    // ===================== BÀI NC1: XIN NHIỀU QUYỀN =====================
    private final ActivityResultLauncher<String[]> xinNhieuQuyen = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                // "result" trả về là một Map<String, Boolean> chứa tên quyền và trạng thái cấp
                Boolean cameraCap = result.get(Manifest.permission.CAMERA);
                Boolean thongBaoCap = result.get(Manifest.permission.POST_NOTIFICATIONS);

                // Xử lý riêng kết quả của quyền Camera
                if (cameraCap != null) {
                    if (cameraCap) {
                        Toast.makeText(this, "Đã cấp quyền CAMERA", Toast.LENGTH_SHORT).show();
                        // Tùy chọn: gọi hàm moCamera() luôn nếu muốn
                    } else {
                        Toast.makeText(this, "Từ chối quyền CAMERA", Toast.LENGTH_SHORT).show();
                    }
                }

                // Xử lý riêng kết quả của quyền Thông báo (nếu có tồn tại)
                if (thongBaoCap != null) {
                    if (thongBaoCap) {
                        Toast.makeText(this, "Đã cấp quyền THÔNG BÁO", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Từ chối quyền THÔNG BÁO", Toast.LENGTH_SHORT).show();
                    }
                }

                // Cập nhật lại TextView hiển thị
                capNhatTrangThai();
            }
    );
    // =====================  HIỂN THỊ TRẠNG THÁI  =====================

    private void capNhatTrangThai() {
        boolean camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean thongBao = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        String s = getString(R.string.status_format,
                camera ? getString(R.string.granted) : getString(R.string.denied),
                thongBao ? getString(R.string.granted) : getString(R.string.denied));

        // THÊM ĐOẠN NÀY: Nếu đã chụp ảnh, nối thêm thông tin ảnh xuống dưới cùng
        if (!thongTinAnh.isEmpty()) {
            s += "\n\n" + thongTinAnh;
        }

        tvTrangThai.setText(s);
        Log.d(TAG, s.replace("\n", " | "));
    }
    // Hàm xin danh bạ NC2
    private void docVaHienThiDanhBa() {
        StringBuilder ketQua = new StringBuilder();
        int dem = 0;

        // Truy vấn dữ liệu danh bạ
        Cursor cursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext() && dem < 5) {
                // Lấy tên
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                String name = cursor.getString(nameIndex);

                // Lấy ID để truy vấn số điện thoại bên trong
                int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                String id = cursor.getString(idIndex);

                // Kiểm tra xem liên hệ này có sđt không
                int hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                if (cursor.getInt(hasPhoneIndex) > 0) {
                    Cursor pCursor = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);

                    if (pCursor != null && pCursor.moveToFirst()) {
                        int phoneIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        String phoneNo = pCursor.getString(phoneIndex);
                        ketQua.append(dem + 1).append(". ").append(name).append("\n   ").append(phoneNo).append("\n\n");
                        pCursor.close();
                    }
                } else {
                    ketQua.append(dem + 1).append(". ").append(name).append("\n   (Không có SĐT)\n\n");
                }
                dem++;
            }
            cursor.close();
        }

        if (ketQua.length() == 0) {
            ketQua.append("Danh bạ máy ảo đang trống.");
        }

        // Hiển thị danh bạ vừa đọc được lên một AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("5 Liên hệ đầu tiên")
                .setMessage(ketQua.toString().trim())
                .setPositiveButton("Đóng", null)
                .show();
    }
    private final ActivityResultLauncher<String> xinQuyenDanhBa = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            duocCap -> {
                if (duocCap) {
                    docVaHienThiDanhBa();
                } else {
                    Toast.makeText(this, "Từ chối quyền đọc danh bạ", Toast.LENGTH_SHORT).show();
                }
            });
}
