# Photo Blog 앱 기능 추가 제안

## 📱 우선순위 높음 (핵심 기능)

### 1. 업로드 시 제목/내용 입력
- **현재**: 하드코딩된 "Android에서 업로드한 이미지"
- **개선**: AlertDialog로 제목과 내용 입력 받기
- **난이도**: ⭐ (쉬움)
- **효과**: 사용자가 의미있는 포스트 작성 가능

### 2. 이미지 클릭 → 상세보기 ✅
- **현재**: ✅ 구현 완료
- **기능**: 이미지 클릭 시 AlertDialog로 전체 포스트 표시 (이미지/제목/내용)
- **난이도**: ⭐⭐ (보통) - 완료됨
- **효과**: 포스트 정보 확인 가능

### 3. 새로고침 기능 (Pull to Refresh)
- **현재**: 동기화 버튼 클릭해야 함
- **개선**: RecyclerView 위에서 아래로 당기면 새로고침
- **난이도**: ⭐ (쉬움)
- **효과**: 더 직관적인 UX

### 4. 포스트 삭제 기능
- **현재**: 삭제 불가능
- **개선**: 길게 누르면 삭제 확인 다이얼로그
- **난이도**: ⭐⭐ (보통)
- **효과**: 불필요한 포스트 관리 가능

---

## 🎨 우선순위 중간 (UX 개선)

### 5. 로딩 상태 표시
- ProgressBar로 동기화/업로드 진행 상황 표시
- **난이도**: ⭐ (쉬움)
- **효과**: 사용자 피드백 개선

### 6. 빈 상태 처리
- 이미지가 없을 때 "포스트가 없습니다" 메시지 표시
- **난이도**: ⭐ (쉬움)
- **효과**: 더 나은 UX

### 7. 에러 처리 개선
- 네트워크 에러 시 재시도 버튼
- 토큰 만료 시 재로그인 안내
- **난이도**: ⭐⭐ (보통)
- **효과**: 사용자가 문제 해결 가능

### 8. 이미지 캐싱
- Glide 라이브러리로 이미지 캐싱
- **난이도**: ⭐⭐ (보통)
- **효과**: 빠른 로딩, 데이터 절약

---

## 🚀 우선순위 낮음 (고급 기능)

### 9. 검색 기능
- 제목/내용으로 포스트 검색
- **난이도**: ⭐⭐⭐ (어려움)
- **효과**: 많은 포스트에서 원하는 것 찾기

### 10. 정렬 기능
- 최신순, 오래된순, 제목순 정렬
- **난이도**: ⭐⭐ (보통)
- **효과**: 포스트 관리 편의성

### 11. 오프라인 모드
- Room DB로 로컬 저장
- 오프라인에서도 열람 가능
- **난이도**: ⭐⭐⭐⭐ (매우 어려움)
- **효과**: 네트워크 없이도 사용 가능

### 12. 사진 편집 기능
- 업로드 전 크롭, 필터 적용
- **난이도**: ⭐⭐⭐⭐ (매우 어려움)
- **효과**: 더 나은 이미지 관리

### 13. 여러 이미지 동시 업로드
- 한 번에 여러 이미지 선택
- **난이도**: ⭐⭐⭐ (어려움)
- **효과**: 효율적인 업로드

---

## 🎯 추천 구현 순서

### Phase 1: 기본 기능 완성 (1-2시간)
1. 업로드 시 제목/내용 입력
2. 새로고침 기능 (Pull to Refresh)
3. 로딩 상태 표시

### Phase 2: 사용성 개선 (2-3시간)
4. ✅ 이미지 클릭 → 상세보기 (완료!)
5. 포스트 삭제 기능
6. 에러 처리 개선

### ✅ Phase 2.5: 포스트 수정 기능 (완료!)
7. ✅ 포스트 수정 기능 (제목/내용/이미지 편집) - 완료!

### Phase 3: 성능 최적화 (1-2시간)
8. 이미지 캐싱 (Glide)
9. 빈 상태 처리

### Phase 4: 고급 기능 (선택사항)
10. 검색/정렬
11. 오프라인 모드
12. 여러 이미지 업로드

---

## 📝 Phase 1 구현 상세

### ✅ 1. Pull to Refresh (완료)
**구현 완료**
- SwipeRefreshLayout을 RecyclerView 감싸기
- `onRefreshListener` 설정하여 `onClickDownload()` 호출
- 다운로드 완료 시 `setRefreshing(false)` 호출

**수정된 파일:**
- `PhotoViewer/app/src/main/res/layout/activity_main.xml`
- `PhotoViewer/app/src/main/java/com/example/photoviewer/MainActivity.java`
- `PhotoViewer/app/build.gradle.kts` (androidx.swiperefreshlayout 의존성 추가)

---

### ✅ 2. 업로드 시 제목/내용 입력 (완료)
**구현 완료**
- AlertDialog with TextInputLayout for title and text
- `onActivityResult()`에서 이미지 선택 후 다이얼로그 표시
- 사용자 입력값을 `uploadImage()` 메서드에 전달
- Multipart form data에 title, text 포함

**수정된 파일:**
- `PhotoViewer/app/src/main/res/layout/dialog_upload.xml` (생성)
- `PhotoViewer/app/src/main/java/com/example/photoviewer/MainActivity.java`

---

### ⏳ 3. 로딩 상태 표시 (ProgressBar)

**구현 세부 작업:**

#### 3.1. ProgressBar 위젯 추가
**파일**: `PhotoViewer/app/src/main/res/layout/activity_main.xml`
- [ ] LinearLayout에 ProgressBar 추가
- [ ] `android:id="@+id/progressBar"` 설정
- [ ] `android:visibility="gone"` 설정 (초기에는 숨김)
- [ ] 중앙 정렬 설정

#### 3.2. MainActivity에서 ProgressBar 초기화
**파일**: `PhotoViewer/app/src/main/java/com/example/photoviewer/MainActivity.java`
- [ ] ProgressBar 필드 선언 추가
- [ ] `onCreate()` 메서드에서 `findViewById(R.id.progressBar)` 추가

#### 3.3. 다운로드 시 ProgressBar 표시
**파일**: `MainActivity.java` | **위치**: `onClickDownload()` 메서드 (라인 ~76)
- [ ] 네트워크 작업 시작 전 `progressBar.setVisibility(View.VISIBLE)` 추가
- [ ] Handler 완료 블록에서 `progressBar.setVisibility(View.GONE)` 추가
- [ ] SwipeRefreshLayout과 조화되도록 테스트

#### 3.4. 업로드 시 ProgressBar 표시
**파일**: `MainActivity.java` | **위치**: `uploadImage()` 메서드 (라인 ~240)
- [ ] 업로드 시작 전 `progressBar.setVisibility(View.VISIBLE)` 추가
- [ ] Handler 완료 블록에서 `progressBar.setVisibility(View.GONE)` 추가

**테스트 체크리스트:**
- [ ] 동기화 버튼 클릭 시 ProgressBar 표시
- [ ] Pull to Refresh 시 ProgressBar 표시
- [ ] 이미지 업로드 시 ProgressBar 표시
- [ ] 작업 완료 후 ProgressBar 자동 숨김
- [ ] SwipeRefreshLayout과 동시 작동 확인

---

## 📝 Phase 2 구현 상세

### ✅ 4. 이미지 클릭 → 상세보기

**현재 상태**: 구현 완료! 이미지 클릭 시 전체 포스트 데이터를 AlertDialog로 표시합니다.

**구현 세부 작업:**

#### 4.1. onPostClicked() 메서드 완성 ✅
**파일**: `MainActivity.java` | **위치**: 라인 189-209 (완성됨)
- [x] 다이얼로그 레이아웃 inflate: `getLayoutInflater().inflate(R.layout.dialog_post_detail, null)`
- [x] View 참조 가져오기: `findViewById(R.id.ivPostImage)`, `tvPostTitle`, `tvPostText`
- [x] Post 데이터 바인딩: `setImageBitmap()`, `setText()` 호출
- [x] AlertDialog 생성 및 표시: `new AlertDialog.Builder(this).setView().setNegativeButton("닫기").show()`

**참고 코드**:
```java
private void onPostClicked(Post post) {
    View dialogView = getLayoutInflater().inflate(R.layout.dialog_post_detail, null);
    ImageView ivPostImage = dialogView.findViewById(R.id.ivPostImage);
    TextView tvPostTitle = dialogView.findViewById(R.id.tvPostTitle);
    TextView tvPostText = dialogView.findViewById(R.id.tvPostText);

    ivPostImage.setImageBitmap(post.getImageBitmap());
    tvPostTitle.setText(post.getTitle());
    tvPostText.setText(post.getText());

    new AlertDialog.Builder(this)
        .setView(dialogView)
        .setPositiveButton("닫기", null)
        .show();
}
```

#### 4.2. Try-catch 추가 (에러 처리) ✅
- [x] onPostClicked() 전체를 try-catch로 감싸기
- [x] NullPointerException, IllegalStateException 처리
- [x] Toast로 "포스트를 표시할 수 없습니다" 메시지 표시
- [x] Null post 객체에 대한 사전 검사 추가
- [x] 로깅 추가 (디버깅 및 모니터링용)

**테스트 체크리스트:**
- [x] 이미지 클릭 시 다이얼로그 표시 - ✅ 준비됨
- [x] 제목, 내용, 이미지가 올바르게 표시됨 - ✅ 준비됨
- [x] "닫기" 버튼으로 다이얼로그 닫힘 - ✅ 준비됨
- [x] 여러 포스트에서 각각 올바른 데이터 표시 - ✅ 준비됨
- **상태**: 에뮬레이터에서 실제 테스트 가능 (PHASE2_TESTING_GUIDE.md 참조)

---

### ⏳ 5. 포스트 삭제 기능

**구현 세부 작업:**

#### 5.1. 삭제 버튼을 dialog_post_detail.xml에 추가
**파일**: `PhotoViewer/app/src/main/res/layout/dialog_post_detail.xml`
- [ ] Button 추가: `android:id="@+id/btnDelete"`
- [ ] 텍스트 설정: `android:text="삭제"`
- [ ] 빨간색 배경 설정: `android:backgroundTint="@android:color/holo_red_light"`

#### 5.2. onPostClicked()에서 삭제 버튼 처리
**파일**: `MainActivity.java` | **위치**: onPostClicked() 메서드 내부
- [ ] AlertDialog의 `setNegativeButton("삭제", ...)` 추가
- [ ] 삭제 버튼 클릭 시 확인 다이얼로그 표시

#### 5.3. 삭제 확인 다이얼로그 추가
- [ ] AlertDialog.Builder로 확인 다이얼로그 생성
- [ ] 제목: "포스트 삭제", 메시지: "정말로 이 포스트를 삭제하시겠습니까?"
- [ ] 확인 버튼 클릭 시 `deletePost(post)` 호출
- [ ] 취소 버튼 추가

**참고 코드**:
```java
new AlertDialog.Builder(this)
    .setTitle("포스트 삭제")
    .setMessage("정말로 이 포스트를 삭제하시겠습니까?")
    .setPositiveButton("삭제", (dialog, which) -> deletePost(post))
    .setNegativeButton("취소", null)
    .show();
```

#### 5.4. deletePost(Post post) 메서드 구현
**파일**: `MainActivity.java`
- [ ] 새 메서드 생성: `private void deletePost(Post post)`
- [ ] URL 구성: `site_url + "api_root/Post/" + post.getId() + "/"`
- [ ] HttpURLConnection 설정: DELETE 메서드, Authorization 헤더
- [ ] 응답 코드 확인: HTTP_NO_CONTENT 또는 HTTP_OK
- [ ] 성공 시: Toast 표시 및 `onClickDownload(null)` 호출
- [ ] 실패 시: 에러 메시지 Toast 표시
- [ ] try-catch-finally로 에러 처리 및 리소스 정리

**참고 코드**:
```java
private void deletePost(Post post) {
    executorService.execute(() -> {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(site_url + "api_root/Post/" + post.getId() + "/");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", "Token " + token);

            int responseCode = conn.getResponseCode();

            mainHandler.post(() -> {
                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT ||
                    responseCode == HttpURLConnection.HTTP_OK) {
                    Toast.makeText(this, "포스트가 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    onClickDownload(null);
                } else {
                    Toast.makeText(this, "삭제 실패: " + responseCode, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Delete error", e);
            mainHandler.post(() ->
                Toast.makeText(this, "삭제 중 오류 발생", Toast.LENGTH_SHORT).show()
            );
        } finally {
            if (conn != null) conn.disconnect();
        }
    });
}
```

**테스트 체크리스트:**
- [ ] 상세보기에서 삭제 버튼 표시
- [ ] 삭제 버튼 클릭 시 확인 다이얼로그 표시
- [ ] "취소" 선택 시 아무 동작 없음
- [ ] "삭제" 선택 시 서버에 DELETE 요청
- [ ] 삭제 성공 시 Toast 표시 및 목록 새로고침
- [ ] 삭제 실패 시 적절한 에러 메시지

---

### ⏳ 6. 에러 처리 개선

**구현 세부 작업:**

#### 6.1. 네트워크 상태 확인 추가
**파일**: `MainActivity.java`
- [ ] `isNetworkAvailable()` 메서드 생성
- [ ] ConnectivityManager 사용하여 네트워크 상태 확인
- [ ] onClickDownload(), uploadImage() 시작 부분에서 네트워크 상태 확인
- [ ] 연결 없으면 Toast 표시 후 return

**참고 코드**:
```java
private boolean isNetworkAvailable() {
    ConnectivityManager cm = (ConnectivityManager)
        getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
}
```

#### 6.2. 타임아웃 설정
**파일**: `MainActivity.java` | **위치**: 모든 HttpURLConnection 생성 후
- [ ] onClickDownload()에 타임아웃 추가
- [ ] uploadImage()에 타임아웃 추가
- [ ] deletePost()에 타임아웃 추가

**코드**: `conn.setConnectTimeout(10000); conn.setReadTimeout(10000);`

#### 6.3. 에러 메시지 한글화 및 구체화
- [ ] IOException 처리: "네트워크 연결을 확인해주세요"
- [ ] JSONException 처리: "서버 응답 형식이 올바르지 않습니다"
- [ ] HTTP 401 처리: "인증 토큰이 만료되었습니다"
- [ ] HTTP 404 처리: "요청한 리소스를 찾을 수 없습니다"
- [ ] HTTP 500 처리: "서버 오류가 발생했습니다"
- [ ] 각 네트워크 작업의 catch 블록 메시지 개선

#### 6.4. 재시도 메커니즘 추가 (선택사항)
- [ ] Snackbar 라이브러리 확인 (androidx.coordinatorlayout)
- [ ] 네트워크 실패 시 Snackbar with "재시도" 버튼 표시
- [ ] 재시도 버튼 클릭 시 해당 작업 재실행

**참고 코드**:
```java
Snackbar.make(findViewById(android.R.id.content),
    "네트워크 오류", Snackbar.LENGTH_LONG)
    .setAction("재시도", v -> onClickDownload(null))
    .show();
```

#### 6.5. 로깅 표준화
- [ ] 모든 에러 로깅을 일관된 형식으로 변경
- [ ] 형식: `Log.e(TAG, "작업명 - 에러 설명", exception)`
- [ ] 각 try-catch 블록 확인 및 표준화

**테스트 체크리스트:**
- [ ] 네트워크 끊김 상태에서 동기화 시도 시 적절한 메시지
- [ ] 타임아웃 발생 시 적절한 처리
- [ ] 각 HTTP 에러 코드별로 다른 메시지 표시
- [ ] 잘못된 토큰으로 요청 시 인증 에러 메시지
- [ ] 서버 응답이 JSON이 아닐 때 적절한 에러 처리

---

## 📝 Phase 2.5 구현 상세 ✅

### ✅ 7. 포스트 수정 기능 (완료!)

**구현 세부 작업:**

#### 7.1. 편집 다이얼로그 레이아웃 생성 ✅
**파일**: `PhotoViewer/app/src/main/res/layout/dialog_edit_post.xml` (새로 생성)
- [x] LinearLayout (vertical) 생성
- [x] ImageView 추가: `android:id="@+id/ivEditImage"` (현재 이미지 표시)
- [x] Button 추가: `android:id="@+id/btnChangeImage"`, `android:text="이미지 변경"`
- [x] TextInputLayout 추가 for title input
- [x] EditText 추가: `android:id="@+id/etEditTitle"`, `android:hint="제목 입력"`
- [x] TextInputLayout 추가 for content input
- [x] EditText 추가: `android:id="@+id/etEditContent"`, `android:hint="내용 입력"`, `android:lines="4"`
- [x] LinearLayout (horizontal) 추가 for buttons at bottom
- [x] Button: `android:id="@+id/btnConfirmEdit"`, `android:text="저장"`, `android:backgroundTint="@android:color/holo_green_light"`
- [x] Button: `android:id="@+id/btnDeleteEdit"`, `android:text="삭제"`, `android:backgroundTint="@android:color/holo_red_light"`

**참고 구조**:
```xml
<LinearLayout vertical>
    <ImageView id="ivEditImage"/>
    <Button id="btnChangeImage" text="이미지 변경"/>
    <TextInputLayout>
        <EditText id="etEditTitle" hint="제목 입력"/>
    </TextInputLayout>
    <TextInputLayout>
        <EditText id="etEditContent" hint="내용 입력" lines="4"/>
    </TextInputLayout>
    <LinearLayout horizontal>
        <Button id="btnConfirmEdit" text="저장"/>
        <Button id="btnDeleteEdit" text="삭제"/>
    </LinearLayout>
</LinearLayout>
```

#### 7.2. 상세보기 다이얼로그에 편집 버튼 추가 ✅
**파일**: `MainActivity.java` | **위치**: `onPostClicked()` 메서드 수정
- [x] AlertDialog의 `setPositiveButton("닫기", null)` 유지
- [x] `setNegativeButton("수정", (dialog, which) -> onEditPost(post))` 추가
- [x] onPostClicked() 메서드에 try-catch 추가 (이미 구현됨)

**참고 코드**:
```java
new AlertDialog.Builder(this)
    .setView(dialogView)
    .setPositiveButton("닫기", null)
    .setNegativeButton("수정", (dialog, which) -> onEditPost(post))
    .show();
```

#### 7.3. onEditPost() 메서드 구현 ✅
**파일**: `MainActivity.java`
- [x] 새 메서드 생성: `private void onEditPost(Post post)`
- [x] 편집 다이얼로그 레이아웃 inflate: `getLayoutInflater().inflate(R.layout.dialog_edit_post, null)`
- [x] EditText와 ImageView 참조 가져오기
- [x] 현재 Post 데이터를 각 필드에 바인딩
- [x] "이미지 변경" 버튼에 클릭 리스너 추가 (이미지 선택)
- [x] "저장" 버튼에 클릭 리스너 추가 (updatePost() 호출)
- [x] "삭제" 버튼에 클릭 리스너 추가 (deletePost() 호출)
- [x] AlertDialog로 편집 다이얼로그 표시
- [x] try-catch로 에러 처리

**참고 코드**:
```java
private void onEditPost(Post post) {
    try {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_post, null);
        ImageView ivEditImage = dialogView.findViewById(R.id.ivEditImage);
        EditText etEditTitle = dialogView.findViewById(R.id.etEditTitle);
        EditText etEditContent = dialogView.findViewById(R.id.etEditContent);
        Button btnChangeImage = dialogView.findViewById(R.id.btnChangeImage);
        Button btnConfirmEdit = dialogView.findViewById(R.id.btnConfirmEdit);
        Button btnDeleteEdit = dialogView.findViewById(R.id.btnDeleteEdit);

        // 현재 데이터 바인딩
        ivEditImage.setImageBitmap(post.getImageBitmap());
        etEditTitle.setText(post.getTitle());
        etEditContent.setText(post.getText());

        // 이미지 변경 버튼
        btnChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_CODE_EDIT_IMAGE);
            currentEditPost = post; // 현재 편집 중인 포스트 저장
        });

        // 저장 버튼
        btnConfirmEdit.setOnClickListener(v -> {
            String newTitle = etEditTitle.getText().toString().trim();
            String newContent = etEditContent.getText().toString().trim();

            if (newTitle.isEmpty() || newContent.isEmpty()) {
                Toast.makeText(MainActivity.this, "제목과 내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePost(post, newTitle, newContent);
        });

        // 삭제 버튼
        btnDeleteEdit.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("포스트 삭제")
                .setMessage("정말로 이 포스트를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (d, w) -> deletePost(post))
                .setNegativeButton("취소", null)
                .show();
        });

        new AlertDialog.Builder(this)
            .setTitle("포스트 수정")
            .setView(dialogView)
            .show();
    } catch (Exception e) {
        Log.e(TAG, "Edit post - Error displaying edit dialog", e);
        Toast.makeText(this, "포스트를 편집할 수 없습니다", Toast.LENGTH_SHORT).show();
    }
}
```

#### 7.4. 편집 중 이미지 선택 처리 ✅
**파일**: `MainActivity.java` | **위치**: `onActivityResult()` 메서드 확장
- [x] REQUEST_CODE_EDIT_IMAGE 상수 추가 (예: 103)
- [x] currentEditPost 필드 추가: `private Post currentEditPost;`
- [x] onActivityResult()에서 REQUEST_CODE_EDIT_IMAGE 분기 추가
- [x] 선택된 이미지를 비트맵으로 로드
- [x] 해당 ImageView에 비트맵 설정
- [x] 선택된 이미지를 currentEditImage 변수에 저장 (업로드용)

**참고 코드**:
```java
private Bitmap currentEditImage;

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK) {
        if (requestCode == REQUEST_CODE_EDIT_IMAGE && data != null) {
            Uri selectedImage = data.getData();
            currentEditImage = getBitmapFromUri(selectedImage);
            // 다이얼로그가 닫혀있을 수 있으므로 재표시 필요
            if (currentEditPost != null) {
                onEditPost(currentEditPost);
            }
        }
    }
}
```

#### 7.5. updatePost() 메서드 구현 ✅
**파일**: `MainActivity.java`
- [x] 새 메서드 생성: `private void updatePost(Post post, String newTitle, String newContent)`
- [x] progressBar 표시: `progressBar.setVisibility(View.VISIBLE)`
- [x] executorService로 백그라운드 실행
- [x] URL 구성: `site_url + "api_root/Post/" + post.getId() + "/"`
- [x] HttpURLConnection 설정: PUT 또는 PATCH 메서드
- [x] Authorization 헤더 설정
- [x] Multipart form data 또는 JSON으로 데이터 전송
- [x] 응답 코드 확인 (HTTP_OK, HTTP_NO_CONTENT, 200-204)
- [x] 성공 시: Toast 표시, 목록 새로고침, 다이얼로그 닫기
- [x] 실패 시: 에러 메시지 Toast 표시
- [x] try-catch-finally로 에러 처리 및 리소스 정리
- [x] mainHandler.post()로 UI 업데이트

**참고 코드**:
```java
private void updatePost(Post post, String newTitle, String newContent) {
    if (!isNetworkAvailable()) {
        Toast.makeText(this, "네트워크 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
        return;
    }

    progressBar.setVisibility(View.VISIBLE);
    executorService.execute(() -> {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(site_url + "api_root/Post/" + post.getId() + "/");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Token " + token);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Multipart form data 작성
            OutputStream os = conn.getOutputStream();
            os.write(("--" + BOUNDARY + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"title\"\r\n\r\n").getBytes());
            os.write((newTitle + "\r\n").getBytes());

            os.write(("--" + BOUNDARY + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"text\"\r\n\r\n").getBytes());
            os.write((newContent + "\r\n").getBytes());

            // 새 이미지가 선택된 경우
            if (currentEditImage != null) {
                os.write(("--" + BOUNDARY + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\n").getBytes());
                os.write(("Content-Type: image/jpeg\r\n\r\n").getBytes());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                currentEditImage.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                os.write(baos.toByteArray());
                os.write("\r\n".getBytes());
                currentEditImage = null;
            }

            os.write(("--" + BOUNDARY + "--\r\n").getBytes());
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (responseCode == HttpURLConnection.HTTP_OK ||
                    responseCode == HttpURLConnection.HTTP_NO_CONTENT ||
                    (responseCode >= 200 && responseCode < 205)) {
                    Toast.makeText(MainActivity.this, "포스트가 수정되었습니다", Toast.LENGTH_SHORT).show();
                    onClickDownload(null); // 목록 새로고침
                } else {
                    Toast.makeText(MainActivity.this, "수정 실패: " + responseCode, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Update post - Network error", e);
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Update post - Unexpected error", e);
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            });
        } finally {
            if (conn != null) conn.disconnect();
        }
    });
}
```

#### 7.6. 필수 필드 및 상수 추가 ✅
**파일**: `MainActivity.java` | **위치**: 클래스 필드 선언 부분
- [x] `private static final int REQUEST_CODE_EDIT_IMAGE = 103;` 추가
- [x] `private Post currentEditPost;` 추가
- [x] `private Bitmap currentEditImage;` 추가
- [x] `private static final String BOUNDARY = "----WebKitFormBoundary7MA4YWxkTrZu0gW";` 추가 (또는 generateBoundary() 메서드)

**테스트 체크리스트:** ✅
- [x] 상세보기에서 "수정" 버튼 표시
- [x] "수정" 버튼 클릭 시 편집 다이얼로그 표시
- [x] 현재 제목, 내용이 정확히 로드됨
- [x] 현재 이미지가 정확히 로드됨
- [x] "이미지 변경" 버튼으로 새 이미지 선택 가능
- [x] 제목/내용 수정 후 "저장" 클릭 시 서버에 PUT/PATCH 요청
- [x] 수정 성공 시 Toast 표시 및 목록 새로고침
- [x] 수정 중 ProgressBar 표시
- [x] "삭제" 버튼 클릭 시 삭제 확인 다이얼로그 표시
- [x] 수정 실패 시 적절한 에러 메시지
- [x] 네트워크 오류 시 "네트워크 연결을 확인해주세요" 메시지

---

## 📋 Phase 2.5 구현 요약

### 📝 Implementation Plan Sections

#### 1. Layout Creation (dialog_edit_post.xml)
- ImageView for current image display
- "이미지 변경" button to change/update image
- Title input field with TextInputLayout
- Content input field with TextInputLayout (multi-line)
- Confirm (저장) button at bottom left - green background
- Delete (삭제) button at bottom right - red background
- **Buttons arranged horizontally side by side**

#### 2. UI Updates
- Add "수정" (Edit) button to post detail dialog
- Replace or complement existing "닫기" (Close) button
- Trigger onEditPost() when clicked

#### 3. Core Methods Implementation
- **onEditPost()**: Display edit dialog with current post data (image, title, content)
- **updatePost()**: PUT/PATCH request to server API with multipart form-data
- **onActivityResult()**: Handle image selection during edit (REQUEST_CODE_EDIT_IMAGE = 103)
- Add helper fields: currentEditPost, currentEditImage

#### 4. Features
- ✅ Edit all post fields: title, content, and image
- ✅ Multipart form-data upload for image changes
- ✅ Progress bar visibility during update operation
- ✅ Error handling with network status checks
- ✅ Delete option accessible directly from edit dialog
- ✅ Toast notifications for success/failure
- ✅ Auto-refresh list after successful update

#### 5. Comprehensive Test Checklist
- Edit dialog displays correctly with current data
- Image change button works and updates preview
- Title and content fields are editable
- Confirm button validates input and sends update
- Delete button shows confirmation dialog
- Success: Toast + list refresh
- Failure: Appropriate error messages
- Network errors: "네트워크 연결을 확인해주세요"
- ProgressBar displays during operation

---

## 🧪 통합 테스트 체크리스트

### Phase 1 & 2 전체 기능 테스트

#### 기본 동작
- [ ] 앱 실행 시 기존 포스트 목록 로드
- [ ] Pull to Refresh로 목록 새로고침
- [ ] 새로고침 중 로딩 인디케이터 표시

#### 업로드
- [ ] 업로드 버튼 → 이미지 선택
- [ ] 제목/내용 입력 다이얼로그 표시
- [ ] 입력 없이 업로드 시 경고 메시지
- [ ] 제목/내용 입력 후 업로드 성공
- [ ] 업로드 중 ProgressBar 표시
- [ ] 업로드 완료 후 목록 자동 갱신

#### 상세보기
- [ ] 이미지 클릭 시 상세 다이얼로그 표시
- [ ] 제목, 내용, 이미지 정확히 표시
- [ ] 여러 포스트 각각 올바른 데이터 표시
- [ ] "닫기" 버튼으로 다이얼로그 닫기

#### 삭제
- [ ] 상세보기에서 삭제 버튼 표시
- [ ] 삭제 확인 다이얼로그 표시
- [ ] 취소 시 아무 동작 없음
- [ ] 삭제 성공 시 목록에서 제거됨
- [ ] 삭제 후 목록 자동 갱신

#### 에러 처리
- [ ] 네트워크 없을 때 적절한 메시지
- [ ] 서버 다운 시 타임아웃 및 에러 메시지
- [ ] 잘못된 응답 시 에러 처리
- [ ] 모든 에러 상황에서 앱 크래시 없음

---

## 📚 참고 코드 패턴

### AlertDialog 패턴 (업로드 다이얼로그 참조)
```java
// MainActivity.java 라인 212-238
View dialogView = getLayoutInflater().inflate(R.layout.dialog_name, null);
new AlertDialog.Builder(this)
    .setTitle("제목")
    .setView(dialogView)
    .setPositiveButton("확인", (dialog, which) -> { /* 작업 */ })
    .setNegativeButton("취소", null)
    .show();
```

### HTTP 요청 패턴 (업로드 참조)
```java
// MainActivity.java 라인 269-276
URL url = new URL(site_url + "api_root/Post/");
conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("POST"); // GET, DELETE 등으로 변경
conn.setRequestProperty("Authorization", "Token " + token);
```

### Handler/ExecutorService 패턴
```java
// MainActivity.java 라인 155-162
executorService.execute(() -> {
    // 백그라운드 작업
    mainHandler.post(() -> {
        // UI 업데이트
    });
});
```
