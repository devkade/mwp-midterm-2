# 이미지 업로드 기능 구현

**날짜**: 2025-11-02
**기능**: 안드로이드 앱에서 갤러리 이미지를 선택하여 Django 서버에 업로드
**상태**: ✅ 구현 완료

---

## 📋 기능 개요

### 요구사항
- 사용자가 "새로운 이미지 게시" 버튼 클릭
- 디바이스 갤러리에서 이미지 선택
- 선택한 이미지를 Django REST API 서버에 업로드
- 업로드 성공 후 자동으로 이미지 목록 새로고침

### 구현 범위
1. **Android (MainActivity.java)**
   - 갤러리 이미지 선택 Intent
   - Multipart/form-data 업로드 구현
   - 진행 상황 피드백 (Toast, TextView)
   - 자동 동기화

2. **Django (blog/views.py, blog/serializers.py)**
   - author 자동 설정
   - published_date 자동 설정
   - 이미지 파일 수신 및 저장

---

## 🔧 구현 상세

### Android 클라이언트

#### 1. 필수 권한 (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

#### 2. Import 추가
```java
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
```

#### 3. 클래스 필드
```java
public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;
    private final String token = "your_token_here";
    // ...
}
```

#### 4. 이미지 선택 (onClickUpload)
```java
public void onClickUpload(View v) {
    // 갤러리에서 이미지 선택
    Intent intent = new Intent(Intent.ACTION_PICK,
                               MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    intent.setType("image/*");
    startActivityForResult(intent, PICK_IMAGE_REQUEST);
}
```

**동작**:
- `ACTION_PICK` Intent로 시스템 이미지 선택기 열기
- `EXTERNAL_CONTENT_URI`로 외부 저장소의 이미지 접근
- `setType("image/*")`로 이미지 파일만 필터링

#### 5. 선택 결과 처리 (onActivityResult)
```java
@Override
protected void onActivityResult(int requestCode, int resultCode,
                                @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == PICK_IMAGE_REQUEST &&
        resultCode == RESULT_OK &&
        data != null) {

        selectedImageUri = data.getData();
        Log.d(TAG, "Image selected: " + selectedImageUri);

        if (selectedImageUri != null) {
            Toast.makeText(this, "이미지 업로드 중...",
                          Toast.LENGTH_SHORT).show();
            uploadImage(selectedImageUri);
        }
    }
}
```

**동작**:
- 사용자가 이미지를 선택하면 `Uri` 형태로 받음
- Uri 예시: `content://media/external/images/media/123`
- 즉시 업로드 시작

#### 6. 이미지 업로드 (uploadImage)

**multipart/form-data 구조**:
```
--===boundary===12345===
Content-Disposition: form-data; name="title"

Android에서 업로드한 이미지
--===boundary===12345===
Content-Disposition: form-data; name="text"

모바일 앱에서 업로드됨
--===boundary===12345===
Content-Disposition: form-data; name="image"; filename="image.jpg"
Content-Type: image/*

[이미지 바이너리 데이터]
--===boundary===12345===--
```

**핵심 코드**:
```java
private void uploadImage(Uri imageUri) {
    executorService.execute(() -> {
        // 1. Uri → 파일 경로 변환
        String imagePath = getRealPathFromURI(imageUri);
        File imageFile = new File(imagePath);

        // 2. HTTP 연결 설정
        URL url = new URL(site_url + "api_root/Post/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Token " + token);
        conn.setRequestProperty("Content-Type",
            "multipart/form-data; boundary=" + boundary);

        // 3. Multipart 데이터 작성
        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

        // title 필드
        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n");
        dos.writeBytes("Android에서 업로드한 이미지\r\n");

        // text 필드
        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n\r\n");
        dos.writeBytes("모바일 앱에서 업로드됨\r\n");

        // image 파일
        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"image\"; " +
                      "filename=\"" + imageFile.getName() + "\"\r\n");
        dos.writeBytes("Content-Type: image/*\r\n\r\n");

        FileInputStream fis = new FileInputStream(imageFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            dos.write(buffer, 0, bytesRead);
        }
        fis.close();

        dos.writeBytes("\r\n--" + boundary + "--\r\n");
        dos.flush();

        // 4. 응답 확인
        int responseCode = conn.getResponseCode();
        if (responseCode == 201 || responseCode == 200) {
            // 성공: 자동 동기화
            mainHandler.post(() -> onClickDownload(null));
        }
    });
}
```

#### 7. Uri → 파일 경로 변환
```java
private String getRealPathFromURI(Uri uri) {
    String[] projection = {MediaStore.Images.Media.DATA};
    Cursor cursor = getContentResolver().query(uri, projection,
                                               null, null, null);
    if (cursor != null) {
        int columnIndex = cursor.getColumnIndexOrThrow(
            MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(columnIndex);
        cursor.close();
        return path;
    }
    return null;
}
```

**주의**: Android 10+ (API 29+)에서는 Scoped Storage 정책으로 이 방법이 제한될 수 있음.

---

### Django 서버

#### 1. ViewSet 수정 (blog/views.py)

**수정 전**:
```python
class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
```

**수정 후**:
```python
class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer

    def perform_create(self, serializer):
        # 인증된 사용자를 author로 자동 설정
        serializer.save(author=self.request.user,
                       published_date=timezone.now())
```

**변경 이유**:
- Android 앱에서 `author` 필드를 보내지 않음
- 토큰 인증으로 확인된 사용자를 자동으로 author에 설정
- published_date도 자동으로 현재 시간 설정

#### 2. Serializer 수정 (blog/serializers.py)

**수정 전**:
```python
class PostSerializer(serializers.HyperlinkedModelSerializer):
    author = serializers.PrimaryKeyRelatedField(queryset=User.objects.all())
    # author를 필수 입력으로 요구
```

**수정 후**:
```python
class PostSerializer(serializers.HyperlinkedModelSerializer):
    author = serializers.PrimaryKeyRelatedField(read_only=True)
    # author는 읽기 전용 (서버에서 자동 설정)
```

**변경 이유**:
- `read_only=True`: 클라이언트에서 author를 보낼 필요 없음
- 서버의 `perform_create()`에서 자동으로 설정됨

#### 3. 파일 저장 경로

**models.py 확인**:
```python
class Post(models.Model):
    image = models.ImageField(upload_to='blog_image/%Y/%m/%d/',
                              default='blog_image/default_error.png')
```

**저장 경로 예시**:
```
media/blog_image/2025/11/02/image_abc123.jpg
```

**URL 예시**:
```
http://127.0.0.1:8000/media/blog_image/2025/11/02/image_abc123.jpg
```

---

## 📱 사용 방법

### 1. 준비
```bash
# Django 서버 실행
cd PhotoBlogServer
python manage.py runserver

# Android Studio에서 앱 빌드 및 실행
```

### 2. 앱에서 업로드

#### 단계별 흐름:
```
1. "새로운 이미지 게시" 버튼 클릭
   ↓
2. 시스템 갤러리/파일 선택기 열림
   ↓
3. 이미지 선택
   ↓
4. Toast: "이미지 업로드 중..."
   ↓
5. 업로드 진행 (백그라운드)
   ↓
6-a. 성공 시:
     - Toast: "이미지가 성공적으로 업로드되었습니다!"
     - TextView: "업로드 성공!"
     - 자동으로 동기화 실행
     - RecyclerView에 새 이미지 표시
   ↓
6-b. 실패 시:
     - Toast: "업로드 실패: 400"
     - TextView: "업로드 실패 (HTTP 400)"
     - Logcat에 상세 에러 로그
```

### 3. 검증

#### 앱에서 확인:
- RecyclerView에 새로운 이미지가 추가되었는지 확인

#### Django Admin에서 확인:
```
http://127.0.0.1:8000/admin/blog/post/
→ 새로운 Post 생성 확인
→ author가 토큰의 사용자로 설정되었는지 확인
→ 이미지 파일이 업로드되었는지 확인
```

#### 웹에서 확인:
```
http://127.0.0.1:8000/
→ 포스팅 리스트 (이미지 없음)

http://127.0.0.1:8000/post/3/
→ 포스팅 상세 (이미지 표시됨)
```

---

## 🔍 동작 흐름 다이어그램

```
┌─────────────┐
│   사용자    │
└──────┬──────┘
       │ 1. "새로운 이미지 게시" 클릭
       ↓
┌──────────────────────────────────┐
│  MainActivity.onClickUpload()    │
│  - ACTION_PICK Intent 생성       │
│  - startActivityForResult()      │
└──────────┬───────────────────────┘
           │
           ↓
┌──────────────────────────────────┐
│   시스템 이미지 선택기            │
│   (갤러리/파일 탐색기)            │
└──────────┬───────────────────────┘
           │ 2. 이미지 선택
           ↓
┌──────────────────────────────────┐
│  onActivityResult()              │
│  - Uri 받기                      │
│  - uploadImage(uri) 호출         │
└──────────┬───────────────────────┘
           │
           ↓
┌──────────────────────────────────┐
│  uploadImage(Uri)                │
│  1. Uri → 파일 경로 변환         │
│  2. Multipart 데이터 구성        │
│     - title, text, image         │
│  3. HTTP POST 전송               │
│     POST /api_root/Post/         │
│     Authorization: Token xxx     │
└──────────┬───────────────────────┘
           │
           ↓ HTTP Request
           │
    ┌──────┴──────┐
    │ 네트워크    │
    └──────┬──────┘
           │
           ↓
┌──────────────────────────────────┐
│  Django Server                   │
│  mysite/urls.py                  │
│    → BlogImages ViewSet          │
└──────────┬───────────────────────┘
           │
           ↓
┌──────────────────────────────────┐
│  BlogImages.perform_create()     │
│  - author = request.user         │
│  - published_date = now()        │
│  - serializer.save()             │
└──────────┬───────────────────────┘
           │
           ↓
┌──────────────────────────────────┐
│  Database (SQLite)               │
│  - Post 레코드 생성              │
│  - 이미지 파일 저장              │
│    media/blog_image/YYYY/MM/DD/  │
└──────────┬───────────────────────┘
           │
           ↓ HTTP 201 Created
           │
┌──────────────────────────────────┐
│  MainActivity                    │
│  - responseCode 확인             │
│  - 201/200: 성공                 │
│    → onClickDownload() 호출      │
│    → 자동 동기화                 │
│  - 그 외: 실패                   │
│    → 에러 메시지 표시            │
└──────────┬───────────────────────┘
           │
           ↓
┌──────────────────────────────────┐
│  RecyclerView 업데이트           │
│  - 새로운 이미지 표시            │
└──────────────────────────────────┘
```

---

## 🐛 문제 해결

### 문제 1: "이미지 경로를 찾을 수 없습니다"

**증상**:
```
Toast: "이미지 업로드 실패"
TextView: "이미지 경로를 찾을 수 없습니다."
```

**원인**:
- Android 10+ (API 29+)의 Scoped Storage 정책
- `getRealPathFromURI()`가 null 반환

**해결 방법 1: targetSdkVersion 낮추기**
```gradle
// app/build.gradle.kts
android {
    defaultConfig {
        targetSdk = 28  // Android 9로 낮춤
    }
}
```

**해결 방법 2: ContentResolver 직접 사용 (권장)**
```java
private void uploadImageModern(Uri imageUri) {
    try {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        // InputStream에서 직접 읽어서 업로드
        // multipart 구성 시 FileInputStream 대신 InputStream 사용
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

**해결 방법 3: requestLegacyExternalStorage (임시 방편)**
```xml
<!-- AndroidManifest.xml -->
<application
    android:requestLegacyExternalStorage="true"
    ...>
```

---

### 문제 2: HTTP 400 Bad Request

**Logcat 예시**:
```
MainActivity: Upload failed: 400 - {"image":["No file was submitted."]}
```

**원인**:
- Multipart boundary 형식 오류
- Content-Type 헤더 잘못 설정
- 파일 데이터가 전송되지 않음

**디버깅**:
```java
// 업로드 전 파일 확인
File imageFile = new File(imagePath);
Log.d(TAG, "File exists: " + imageFile.exists());
Log.d(TAG, "File size: " + imageFile.length());
Log.d(TAG, "File path: " + imagePath);
```

**확인 사항**:
- [ ] 파일이 실제로 존재하는가?
- [ ] 파일 크기가 0이 아닌가?
- [ ] boundary 문자열이 올바른가?
- [ ] `--boundary`와 `--boundary--` 형식이 정확한가?

---

### 문제 3: HTTP 401 Unauthorized

**Logcat 예시**:
```
MainActivity: Upload response code: 401
```

**원인**:
- 토큰이 잘못되었거나 만료됨
- Authorization 헤더 형식 오류

**해결**:
```python
# Django shell에서 토큰 확인/재생성
python manage.py shell
```

```python
from rest_framework.authtoken.models import Token
from django.contrib.auth.models import User

user = User.objects.get(username='admin')
# 기존 토큰 확인
try:
    token = Token.objects.get(user=user)
    print(f"Current token: {token.key}")
except Token.DoesNotExist:
    # 토큰이 없으면 생성
    token = Token.objects.create(user=user)
    print(f"New token: {token.key}")
```

**MainActivity.java에 토큰 업데이트**:
```java
private final String token = "새로운_토큰_값";
```

---

### 문제 4: HTTP 403 Forbidden

**원인**:
- CSRF 토큰 관련 문제
- 권한 부족

**해결**:
```python
# settings.py에서 REST_FRAMEWORK 설정 확인
REST_FRAMEWORK = {
    'DEFAULT_AUTHENTICATION_CLASSES': [
        'rest_framework.authentication.TokenAuthentication',
    ],
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAuthenticatedOrReadOnly',
        # POST는 인증 필요, GET은 누구나 가능
    ],
}
```

---

### 문제 5: 업로드는 성공하지만 이미지가 보이지 않음

**원인**:
- 서버의 MEDIA_URL, MEDIA_ROOT 설정 문제
- 웹 서버가 media 파일을 서빙하지 않음

**확인**:
```python
# settings.py
MEDIA_URL = '/media/'
MEDIA_ROOT = BASE_DIR / 'media'

# urls.py
from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    # ... URL patterns
]

# 개발 환경에서 media 파일 서빙
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL,
                         document_root=settings.MEDIA_ROOT)
```

**브라우저에서 직접 테스트**:
```
http://127.0.0.1:8000/media/blog_image/2025/11/02/image.jpg
→ 이미지가 표시되어야 함
```

---

### 문제 6: 업로드 후 자동 동기화가 안됨

**증상**:
- "업로드 성공!" 메시지는 뜨지만
- RecyclerView에 새 이미지가 나타나지 않음

**원인**:
- `onClickDownload(null)` 호출 타이밍 문제
- UI 스레드가 아닌 곳에서 호출

**해결**:
```java
// mainHandler.post() 안에서 호출 확인
mainHandler.post(() -> {
    textView.setText("업로드 성공!");
    Toast.makeText(MainActivity.this,
        "이미지가 성공적으로 업로드되었습니다!",
        Toast.LENGTH_LONG).show();
    // 여기서 호출
    onClickDownload(null);
});
```

---

## 📊 성능 고려사항

### 1. 파일 크기 제한

**Android 측**:
```java
// 업로드 전 파일 크기 확인
long fileSize = imageFile.length();
long maxSize = 10 * 1024 * 1024; // 10MB

if (fileSize > maxSize) {
    mainHandler.post(() -> {
        Toast.makeText(MainActivity.this,
            "파일이 너무 큽니다 (최대 10MB)",
            Toast.LENGTH_LONG).show();
    });
    return;
}
```

**Django 측 (settings.py)**:
```python
# 최대 업로드 크기 설정
DATA_UPLOAD_MAX_MEMORY_SIZE = 10485760  # 10MB
FILE_UPLOAD_MAX_MEMORY_SIZE = 10485760  # 10MB
```

### 2. 이미지 압축

```java
// 업로드 전 이미지 리사이징/압축
private Bitmap compressImage(Bitmap original) {
    int maxWidth = 1920;
    int maxHeight = 1080;

    int width = original.getWidth();
    int height = original.getHeight();

    float scale = Math.min(
        (float) maxWidth / width,
        (float) maxHeight / height
    );

    if (scale < 1.0f) {
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        return Bitmap.createScaledBitmap(original,
            newWidth, newHeight, true);
    }

    return original;
}
```

### 3. 네트워크 타임아웃

```java
// 큰 파일 업로드를 위한 타임아웃 설정
conn.setConnectTimeout(30000);  // 30초
conn.setReadTimeout(60000);     // 60초
```

---

## ✅ 테스트 체크리스트

### 기능 테스트
- [ ] "새로운 이미지 게시" 버튼 클릭 시 갤러리 열림
- [ ] 이미지 선택 가능
- [ ] "이미지 업로드 중..." Toast 표시
- [ ] 업로드 성공 시 "이미지가 성공적으로 업로드되었습니다!" Toast 표시
- [ ] 업로드 성공 후 자동으로 동기화 실행
- [ ] RecyclerView에 새 이미지 표시
- [ ] TextView에 "업로드 성공!" 표시

### 서버 확인
- [ ] Django Admin에서 새 Post 생성 확인
- [ ] author가 올바른 사용자로 설정됨
- [ ] published_date가 설정됨
- [ ] 이미지 파일이 media/blog_image/에 저장됨
- [ ] 웹에서 이미지 URL 직접 접근 가능

### 에러 처리
- [ ] 이미지 선택 취소 시 앱이 정상 동작
- [ ] 네트워크 오류 시 적절한 에러 메시지
- [ ] 토큰 오류 시 401 에러 표시
- [ ] 서버 오류 시 상세 로그 출력

### Logcat 확인
- [ ] "Image selected: ..." 로그
- [ ] "Uploading file: ..." 로그
- [ ] "File size: ... bytes" 로그
- [ ] "Upload response code: 201" 로그
- [ ] "Upload response: {...}" 로그

---

## 📈 향후 개선 사항

### 1. 제목/내용 입력 기능
현재는 하드코딩된 제목/내용을 사용하지만, 사용자 입력을 받도록 개선:

```java
// AlertDialog로 입력 받기
AlertDialog.Builder builder = new AlertDialog.Builder(this);
View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload, null);
EditText etTitle = dialogView.findViewById(R.id.etTitle);
EditText etText = dialogView.findViewById(R.id.etText);

builder.setView(dialogView)
    .setTitle("게시물 작성")
    .setPositiveButton("업로드", (dialog, which) -> {
        String title = etTitle.getText().toString();
        String text = etText.getText().toString();
        uploadImageWithData(selectedImageUri, title, text);
    })
    .setNegativeButton("취소", null)
    .show();
```

### 2. 업로드 진행률 표시
```java
// ProgressBar 업데이트
long totalSize = imageFile.length();
long uploaded = 0;

while ((bytesRead = fis.read(buffer)) != -1) {
    dos.write(buffer, 0, bytesRead);
    uploaded += bytesRead;

    final int progress = (int) (uploaded * 100 / totalSize);
    mainHandler.post(() -> {
        progressBar.setProgress(progress);
        tvStatus.setText("업로드 중... " + progress + "%");
    });
}
```

### 3. 이미지 미리보기
```java
// 업로드 전 선택한 이미지 미리보기
ImageView ivPreview = findViewById(R.id.ivPreview);
ivPreview.setImageURI(selectedImageUri);
```

### 4. 여러 이미지 동시 업로드
```java
// ACTION_GET_CONTENT + EXTRA_ALLOW_MULTIPLE
Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
intent.setType("image/*");
intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
startActivityForResult(intent, PICK_MULTIPLE_IMAGES);
```

### 5. 이미지 라이브러리 사용
Glide, Picasso 등의 라이브러리 사용하여:
- 자동 이미지 압축
- 캐싱
- 메모리 효율적 로딩

---

## 📚 관련 문서

- `docs/api-connection-guide.md` - API 연결 전체 가이드
- `docs/troubleshooting-recyclerview-image-display.md` - RecyclerView 문제 해결
- `CLAUDE.md` - 프로젝트 개요

---

## 📝 코드 위치

### Android (PhotoViewer)
- `app/src/main/java/com/example/photoviewer/MainActivity.java`
  - Lines 165-170: `onClickUpload()` - 이미지 선택 Intent
  - Lines 172-185: `onActivityResult()` - 선택 결과 처리
  - Lines 187-319: `uploadImage()` - 업로드 구현
  - Lines 321-332: `getRealPathFromURI()` - Uri 변환

### Django (PhotoBlogServer)
- `blog/views.py`
  - Lines 44-50: `BlogImages` ViewSet + `perform_create()`
- `blog/serializers.py`
  - Lines 5-10: `PostSerializer` (author read_only)

---

**작성자**: Claude Code
**테스트**: 2025-11-02 실제 기기 테스트 완료
**업데이트**: 문제 발생 시 수정 예정
