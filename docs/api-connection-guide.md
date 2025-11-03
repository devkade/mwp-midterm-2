# API 연결 가이드: Django ↔ Android

**프로젝트**: Photo Blog Mobile/Web Service
**업데이트**: 2025-11-02

---

## 📡 API 엔드포인트 연결 맵

### 전체 연결 흐름
```
Android App (MainActivity.java)
    ↓
HTTP GET: http://10.0.2.2:8000/api_root/Post/
    ↓ (Authorization: Token <token>)
    ↓
Django Server
    ↓
mysite/urls.py → router.register('Post', views.BlogImages)
    ↓
blog/views.py → BlogImages ViewSet
    ↓
blog/serializers.py → PostSerializer
    ↓
blog/models.py → Post model
    ↓
JSON Response + Image URLs
    ↓
Android App downloads each image
    ↓
RecyclerView displays images
```

---

## 🔧 서버 설정 (Django)

### 1. URL 라우팅

#### mysite/urls.py (실제 사용되는 설정)
```python
from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static
from rest_framework import routers
from blog import views

# REST API Router
router = routers.DefaultRouter()
router.register('Post', views.BlogImages)  # ← 실제 라우터 등록

urlpatterns = [
    path('', views.post_list, name='post_list'),
    path('post/<int:pk>/', views.post_detail, name='post_detail'),
    path('post/new/', views.post_new, name='post_new'),
    path('post/<int:pk>/edit/', views.post_edit, name='post_edit'),
    path('api_root/', include(router.urls)),  # ← API 엔드포인트
    path('admin/', admin.site.urls),
    path('api-token-auth/', obtain_auth_token),
]

# Media files serving
urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
```

**중요**: `blog/urls.py`가 존재하지만 **실제로 사용되지 않습니다**. 메인 `mysite/urls.py`에서 직접 라우터를 등록하고 있습니다.

#### blog/urls.py (사용되지 않음)
```python
# 이 파일은 mysite/urls.py에 include되지 않아 사용되지 않습니다
# 삭제하거나 무시해도 됩니다
router = routers.DefaultRouter()
router.register('Post', views.BlogImages)
```

### 2. ViewSet

#### blog/views.py
```python
class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
```

**제공 엔드포인트**:
- `GET /api_root/Post/` - 모든 포스트 조회
- `GET /api_root/Post/{id}/` - 특정 포스트 조회
- `POST /api_root/Post/` - 새 포스트 생성
- `PUT /api_root/Post/{id}/` - 포스트 수정
- `DELETE /api_root/Post/{id}/` - 포스트 삭제

### 3. Serializer

#### blog/serializers.py
```python
class PostSerializer(serializers.HyperlinkedModelSerializer):
    author = serializers.PrimaryKeyRelatedField(queryset=User.objects.all())

    class Meta:
        model = Post
        fields = ('author', 'title', 'text', 'created_date', 'published_date', 'image')
```

**JSON 응답 예시**:
```json
[
  {
    "author": 1,
    "title": "포스팅",
    "text": "이미지 및 포스팅 테스트",
    "created_date": "2025-11-02T17:44:49+09:00",
    "published_date": "2025-11-02T17:45:25+09:00",
    "image": "http://127.0.0.1:8000/media/blog_image/2025/11/02/image.png"
  }
]
```

### 4. 인증 토큰

#### 토큰 생성
```bash
cd PhotoBlogServer
python manage.py shell
```

```python
from rest_framework.authtoken.models import Token
from django.contrib.auth.models import User

user = User.objects.get(username='admin')
token, created = Token.objects.get_or_create(user=user)
print(f"Token: {token.key}")
```

#### 토큰 사용
```bash
# HTTP 헤더에 추가
Authorization: Token <token_key>
```

---

## 📱 클라이언트 설정 (Android)

### 1. MainActivity 설정

#### 중요 상수
```java
private final String site_url = "http://10.0.2.2:8000/";
// 10.0.2.2 = Android 에뮬레이터에서 호스트 머신의 localhost

private static final String AUTH_TOKEN = "your_token_here";
// Django에서 생성한 토큰 입력
```

**실제 기기 테스트 시**:
```java
// 에뮬레이터
private final String site_url = "http://10.0.2.2:8000/";

// 실제 기기 (같은 Wi-Fi)
private final String site_url = "http://192.168.x.x:8000/";

// 배포된 서버
private final String site_url = "https://yourusername.pythonanywhere.com/";
```

### 2. 네트워크 권한

#### AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

#### network_security_config.xml
```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**주의**: HTTP 통신 허용 (개발 환경용). 프로덕션에서는 HTTPS 사용 권장.

### 3. 이미지 다운로드 로직

#### MainActivity.java - onClickDownload()
```java
public void onClickDownload(View v) {
    Toast.makeText(getApplicationContext(), "이미지 동기화 중...", Toast.LENGTH_SHORT).show();
    executorService.execute(() -> {
        List<Bitmap> downloadedImages = new ArrayList<>();
        try {
            // 1. API 엔드포인트 호출
            URL url = new URL(site_url + "api_root/Post/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Token " + token);
            conn.setRequestMethod("GET");

            // 2. JSON 응답 파싱
            JSONArray aryJson = new JSONArray(result.toString());

            // 3. 각 포스트의 이미지 다운로드
            for (int i = 0; i < aryJson.length(); i++) {
                JSONObject post_json = aryJson.getJSONObject(i);
                String imageUrl = post_json.getString("image");

                // 이미지 다운로드
                URL myImageUrl = new URL(imageUrl);
                HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();
                InputStream imgStream = imgConn.getInputStream();
                Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);

                if (imageBitmap != null) {
                    downloadedImages.add(imageBitmap);
                }
            }

            // 4. UI 업데이트
            mainHandler.post(() -> {
                imageList.clear();
                imageList.addAll(downloadedImages);
                imageAdapter.notifyDataSetChanged();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
}
```

---

## 🧪 테스트 방법

### 1. 서버 테스트

#### Django 서버 실행
```bash
cd PhotoBlogServer
python manage.py runserver
```

#### 브라우저에서 API 확인
```
http://127.0.0.1:8000/api_root/Post/
```

예상 결과: DRF Browsable API 화면에 JSON 데이터 표시

#### curl로 테스트
```bash
curl -H "Authorization: Token your_token_here" \
     http://127.0.0.1:8000/api_root/Post/
```

#### telnet으로 테스트
```bash
telnet 127.0.0.1 8000
```
```http
GET /api_root/Post/ HTTP/1.1
Host: 127.0.0.1:8000
Authorization: Token your_token_here

```

### 2. 안드로이드 앱 테스트

#### Logcat 필터 설정
1. Android Studio > Logcat
2. 필터: `MainActivity`
3. Log level: `Debug`

#### 동기화 버튼 클릭
예상 로그:
```
MainActivity: Total posts received: 2
MainActivity: Post #1: 포스팅
MainActivity: Image URL: http://10.0.2.2:8000/media/...
MainActivity: Image response code: 200
MainActivity: ✓ Image #1 downloaded successfully
MainActivity: Post #2: 커피커피
MainActivity: Image URL: http://10.0.2.2:8000/media/...
MainActivity: Image response code: 200
MainActivity: ✓ Image #2 downloaded successfully
MainActivity: Total images downloaded: 2
```

---

## 🐛 일반적인 문제 해결

### 문제 1: "Unable to resolve host"
**원인**: 네트워크 연결 문제 또는 잘못된 URL

**해결**:
```java
// 에뮬레이터: localhost → 10.0.2.2
private final String site_url = "http://10.0.2.2:8000/";

// 실제 기기: 호스트 IP 주소 사용
private final String site_url = "http://192.168.1.100:8000/";
```

Django 서버가 실행 중인지 확인:
```bash
python manage.py runserver
```

### 문제 2: "CLEARTEXT communication not permitted"
**원인**: Android 9+ 에서 HTTP 통신 기본 차단

**해결**: `network_security_config.xml` 확인
```xml
<base-config cleartextTrafficPermitted="true">
```

### 문제 3: HTTP 401 Unauthorized
**원인**: 토큰이 잘못되었거나 만료됨

**해결**:
```python
# Django shell에서 토큰 재생성
from rest_framework.authtoken.models import Token
token = Token.objects.get(user=user)
token.delete()
token = Token.objects.create(user=user)
print(token.key)
```

### 문제 4: HTTP 404 Not Found
**원인**: URL 경로가 잘못됨

**확인**:
```
✓ http://10.0.2.2:8000/api_root/Post/  (올바름)
✗ http://10.0.2.2:8000/api/Post/       (잘못됨)
✗ http://10.0.2.2:8000/Post/           (잘못됨)
```

### 문제 5: 이미지 URL이 상대 경로로 반환됨
**원인**: Serializer에서 절대 URL 생성하지 않음

**해결**: `PostSerializer` 수정
```python
class PostSerializer(serializers.ModelSerializer):
    image = serializers.SerializerMethodField()

    def get_image(self, obj):
        if obj.image:
            request = self.context.get('request')
            return request.build_absolute_uri(obj.image.url)
        return None
```

### 문제 6: 이미지 다운로드 시 타임아웃
**원인**: 이미지 파일이 너무 크거나 네트워크가 느림

**해결**:
```java
conn.setConnectTimeout(10000);  // 10초
conn.setReadTimeout(10000);     // 10초
```

---

## 📊 API 응답 시간 최적화

### 서버 측 (Django)

#### 1. 쿼리 최적화
```python
class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.select_related('author').all()
    serializer_class = PostSerializer
```

#### 2. 페이지네이션 추가
```python
# settings.py
REST_FRAMEWORK = {
    'DEFAULT_PAGINATION_CLASS': 'rest_framework.pagination.PageNumberPagination',
    'PAGE_SIZE': 10
}
```

### 클라이언트 측 (Android)

#### 1. 병렬 이미지 다운로드
```java
ExecutorService imageDownloadExecutor = Executors.newFixedThreadPool(4);
for (int i = 0; i < aryJson.length(); i++) {
    final int index = i;
    imageDownloadExecutor.execute(() -> {
        // 이미지 다운로드
    });
}
```

#### 2. 이미지 캐싱
Glide 라이브러리 사용 권장:
```gradle
implementation 'com.github.bumptech.glide:glide:4.16.0'
```

---

## 🔒 보안 고려사항

### 1. 토큰 보안
**현재 (개발 환경)**:
```java
private String token = "bf46b8f9337d1d27b4ef2511514c798be1a954b8";  // 하드코딩 ❌
```

**권장 (프로덕션)**:
```java
// BuildConfig 사용
private String token = BuildConfig.API_TOKEN;
```

```gradle
// build.gradle
android {
    buildTypes {
        debug {
            buildConfigField "String", "API_TOKEN", "\"your_dev_token\""
        }
        release {
            buildConfigField "String", "API_TOKEN", "\"your_prod_token\""
        }
    }
}
```

### 2. HTTPS 사용
프로덕션 환경에서는 반드시 HTTPS 사용:
```java
private final String site_url = "https://yourdomain.com/";
```

```xml
<!-- network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">yourdomain.com</domain>
    </domain-config>
</network-security-config>
```

---

## 📝 체크리스트

### 서버 설정
- [ ] Django 서버 실행 중
- [ ] API 엔드포인트 브라우저에서 접근 가능
- [ ] 최소 1개 이상의 Post 데이터 존재 (이미지 포함)
- [ ] 토큰 생성 완료
- [ ] ALLOWED_HOSTS 설정 확인
- [ ] MEDIA_URL, MEDIA_ROOT 설정 확인
- [ ] media 파일이 static으로 서빙되는지 확인

### 클라이언트 설정
- [ ] INTERNET 권한 추가
- [ ] network_security_config.xml 설정
- [ ] 올바른 URL 사용 (에뮬레이터: 10.0.2.2)
- [ ] 토큰 MainActivity에 입력
- [ ] RecyclerView LayoutManager 설정
- [ ] ImageAdapter 연결

### 테스트
- [ ] 서버 API 브라우저에서 테스트
- [ ] 안드로이드 앱에서 동기화 버튼 클릭
- [ ] Logcat에서 로그 확인
- [ ] RecyclerView에 이미지 표시 확인

---

## 📖 관련 문서

- `docs/troubleshooting-recyclerview-image-display.md` - RecyclerView 표시 문제 해결
- `docs/plans/2025-11-02-photo-blog-implementation.md` - 전체 구현 계획
- `CLAUDE.md` - 프로젝트 개요 및 명령어

---

**작성자**: Claude Code
**검증**: 2025-11-02 실제 테스트 완료
