# Photo Blog Mobile/Web Service Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a RESTful photo blog service with Django backend and Android client for uploading and viewing photos with authentication.

**Architecture:** Django REST Framework provides token-authenticated API endpoints for POST (multipart image upload) and GET (JSON list with image URLs). Android client uses AsyncTask for network operations, RecyclerView for display, and HttpURLConnection for API communication.

**Tech Stack:** Django 3.x, Django REST Framework, Python 3.x, Android SDK, Java, PythonAnywhere for deployment

---

## Task 1: Django Project Setup

**Files:**
- Create: `mysite/settings.py`
- Create: `mysite/urls.py`
- Create: `mysite/wsgi.py`
- Create: `manage.py`
- Create: `requirements.txt`

**Step 1: Create Django project structure**

```bash
django-admin startproject mysite .
```

Expected: Django project created with manage.py in current directory

**Step 2: Create requirements.txt**

Create file: `requirements.txt`

```txt
Django==3.2.23
djangorestframework==3.14.0
Pillow==10.1.0
django-cors-headers==4.3.1
```

**Step 3: Install dependencies**

```bash
python -m venv djangoenv
source djangoenv/bin/activate  # On Windows: djangoenv\Scripts\activate
pip install -r requirements.txt
```

Expected: All packages installed successfully

**Step 4: Configure settings.py for development**

Modify: `mysite/settings.py`

Add to INSTALLED_APPS:
```python
INSTALLED_APPS = [
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'rest_framework',
    'rest_framework.authtoken',
    'corsheaders',
    'blog',
]
```

Add to MIDDLEWARE (before CommonMiddleware):
```python
MIDDLEWARE = [
    'django.middleware.security.SecurityMiddleware',
    'corsheaders.middleware.CorsMiddleware',  # Add this
    'django.middleware.common.CommonMiddleware',
    # ... rest of middleware
]
```

Update ALLOWED_HOSTS:
```python
ALLOWED_HOSTS = ['127.0.0.1', 'localhost', '10.0.2.2', '*']
```

Add at end of file:
```python
# REST Framework settings
REST_FRAMEWORK = {
    'DEFAULT_AUTHENTICATION_CLASSES': [
        'rest_framework.authentication.TokenAuthentication',
    ],
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAuthenticatedOrReadOnly',
    ],
}

# CORS settings for development
CORS_ALLOW_ALL_ORIGINS = True

# Media files
MEDIA_URL = '/media/'
MEDIA_ROOT = os.path.join(BASE_DIR, 'media')

# Static files
STATIC_URL = '/static/'
STATIC_ROOT = os.path.join(BASE_DIR, 'static')
```

Add import at top:
```python
import os
```

**Step 5: Verify settings**

```bash
python manage.py check
```

Expected: "System check identified no issues (0 silenced)."

**Step 6: Commit**

```bash
git add .
git commit -m "feat: initial Django project setup with DRF"
```

---

## Task 2: Blog App and Models

**Files:**
- Create: `blog/models.py`
- Create: `blog/__init__.py`
- Create: `blog/apps.py`
- Create: `blog/admin.py`

**Step 1: Create blog app**

```bash
python manage.py startapp blog
```

Expected: blog directory created with Django app structure

**Step 2: Write Post model**

Modify: `blog/models.py`

```python
from django.conf import settings
from django.db import models
from django.utils import timezone


class Post(models.Model):
    author = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    title = models.CharField(max_length=200)
    text = models.TextField()
    created_date = models.DateTimeField(default=timezone.now)
    published_date = models.DateTimeField(blank=True, null=True)
    image = models.ImageField(upload_to='images/', blank=True, null=True)

    def publish(self):
        self.published_date = timezone.now()
        self.save()

    def __str__(self):
        return self.title

    class Meta:
        ordering = ['-created_date']
```

**Step 3: Register model in admin**

Modify: `blog/admin.py`

```python
from django.contrib import admin
from .models import Post


@admin.ModelAdmin
class PostAdmin(admin.ModelAdmin):
    list_display = ('title', 'author', 'created_date', 'published_date')
    list_filter = ('created_date', 'published_date', 'author')
    search_fields = ('title', 'text')


admin.site.register(Post, PostAdmin)
```

**Step 4: Create and run migrations**

```bash
python manage.py makemigrations blog
python manage.py migrate
```

Expected: Migrations created and applied successfully

**Step 5: Create superuser**

```bash
python manage.py createsuperuser
```

Input: username=admin, email=admin@test.com, password=admin123
Expected: Superuser created successfully

**Step 6: Verify admin access**

```bash
python manage.py runserver
```

Open browser: http://127.0.0.1:8000/admin
Login with admin/admin123
Expected: Can see Posts in admin interface

**Step 7: Commit**

```bash
git add blog/
git commit -m "feat: add Post model with image support"
```

---

## Task 3: REST API Serializers and ViewSets

**Files:**
- Create: `blog/serializers.py`
- Create: `blog/views.py`
- Modify: `blog/urls.py`

**Step 1: Create serializer**

Create: `blog/serializers.py`

```python
from rest_framework import serializers
from .models import Post


class PostSerializer(serializers.ModelSerializer):
    author = serializers.ReadOnlyField(source='author.username')
    image = serializers.ImageField(required=False, allow_null=True, use_url=True)

    class Meta:
        model = Post
        fields = ['id', 'author', 'title', 'text', 'created_date',
                  'published_date', 'image']
        read_only_fields = ['created_date']

    def to_representation(self, instance):
        """Ensure image URL is absolute"""
        representation = super().to_representation(instance)
        if instance.image:
            request = self.context.get('request')
            if request is not None:
                representation['image'] = request.build_absolute_uri(instance.image.url)
        return representation
```

**Step 2: Create ViewSet**

Modify: `blog/views.py`

```python
from rest_framework import viewsets, status
from rest_framework.authentication import TokenAuthentication
from rest_framework.permissions import IsAuthenticatedOrReadOnly
from rest_framework.response import Response
from .models import Post
from .serializers import PostSerializer


class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
    authentication_classes = [TokenAuthentication]
    permission_classes = [IsAuthenticatedOrReadOnly]

    def perform_create(self, serializer):
        serializer.save(author=self.request.user, published_date=timezone.now())

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        self.perform_create(serializer)
        headers = self.get_success_headers(serializer.data)
        return Response(serializer.data, status=status.HTTP_201_CREATED, headers=headers)
```

Add import at top:
```python
from django.utils import timezone
```

**Step 3: Create URL routing**

Create: `blog/urls.py`

```python
from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import PostViewSet

router = DefaultRouter()
router.register(r'Post', PostViewSet, basename='post')

urlpatterns = [
    path('', router.urls),
]
```

**Step 4: Update main URLs**

Modify: `mysite/urls.py`

```python
from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api_root/', include('blog.urls')),
    path('api-auth/', include('rest_framework.urls')),
]

# Serve media files in development
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
```

**Step 5: Create auth token for superuser**

```bash
python manage.py shell
```

```python
from django.contrib.auth.models import User
from rest_framework.authtoken.models import Token

user = User.objects.get(username='admin')
token, created = Token.objects.get_or_create(user=user)
print(f"Token for admin: {token.key}")
exit()
```

Expected: Token printed (save this for later)

**Step 6: Test API endpoint**

```bash
python manage.py runserver
```

Open browser: http://127.0.0.1:8000/api_root/Post/
Expected: DRF browsable API showing empty list []

**Step 7: Commit**

```bash
git add blog/ mysite/
git commit -m "feat: add REST API with PostViewSet and serializer"
```

---

## Task 4: API Verification with Telnet

**Files:**
- Create: `docs/api-verification.md`

**Step 1: Create test post via admin**

- Start server: `python manage.py runserver`
- Open: http://127.0.0.1:8000/admin/blog/post/
- Click "Add Post"
- Fill in: title="Test Post", text="Test content", select admin as author
- Upload a test image
- Save

Expected: Post created successfully

**Step 2: Create telnet test documentation**

Create: `docs/api-verification.md`

```markdown
# API Verification with Telnet

## GET Request to List Posts

### Command:
```bash
telnet 127.0.0.1 8000
```

### HTTP Request (type exactly):
```
GET /api_root/Post/ HTTP/1.1
Host: 127.0.0.1:8000
Authorization: Token YOUR_TOKEN_HERE
Accept: application/json

```

**Note:** Press Enter twice after the last line to send blank line

### Expected Response:
```
HTTP/1.1 200 OK
Content-Type: application/json
...

[
  {
    "id": 1,
    "author": "admin",
    "title": "Test Post",
    "text": "Test content",
    "created_date": "2025-11-02T...",
    "published_date": "2025-11-02T...",
    "image": "http://127.0.0.1:8000/media/images/test.jpg"
  }
]
```

## Validation Steps:

1. Copy JSON response
2. Go to: https://codebeautify.org/jsonviewer
3. Paste JSON
4. Click "Validate"
5. Expected: "Valid JSON" message

## Screenshots Required:

1. Telnet terminal showing request and response
2. JSON validator showing "Valid JSON"
3. Browser showing API response at http://127.0.0.1:8000/api_root/Post/
```

**Step 3: Perform telnet test**

```bash
python manage.py runserver
# In another terminal:
telnet 127.0.0.1 8000
```

Type the GET request from docs
Expected: JSON response with post data

**Step 4: Validate JSON**

Copy JSON response to https://codebeautify.org/jsonviewer
Expected: Valid JSON confirmation

**Step 5: Take screenshots**

Required screenshots:
1. `docs/screenshots/telnet-request.png` - Terminal with telnet session
2. `docs/screenshots/json-validation.png` - JSON validator result
3. `docs/screenshots/browser-api.png` - Browser showing API response

**Step 6: Commit**

```bash
git add docs/
git commit -m "docs: add API verification with telnet instructions"
```

---

## Task 5: Android Project Setup

**Files:**
- Create: `PhotoBlogApp/app/src/main/AndroidManifest.xml`
- Create: `PhotoBlogApp/app/build.gradle`
- Create: `PhotoBlogApp/settings.gradle`
- Create: `PhotoBlogApp/gradle.properties`

**Step 1: Create Android project structure**

In Android Studio:
1. File → New → New Project
2. Select "Empty Activity"
3. Name: PhotoBlogApp
4. Package: com.example.photoblog
5. Language: Java
6. Minimum SDK: API 24 (Android 7.0)
7. Click Finish

Expected: Project created with MainActivity

**Step 2: Configure AndroidManifest.xml**

Modify: `PhotoBlogApp/app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.photoblog">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.CAMERA" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.PhotoBlogApp"
        android:networkSecurityConfig="@xml/network_security_config"
        android:requestLegacyExternalStorage="true">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

**Step 3: Create network security config**

Create: `PhotoBlogApp/app/src/main/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">127.0.0.1</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

**Step 4: Configure build.gradle**

Modify: `PhotoBlogApp/app/build.gradle`

```gradle
plugins {
    id 'com.android.application'
}

android {
    compileSdk 33

    defaultConfig {
        applicationId "com.example.photoblog"
        minSdk 24
        targetSdk 33
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.1'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

**Step 5: Sync Gradle**

In Android Studio: File → Sync Project with Gradle Files
Expected: Sync successful

**Step 6: Commit**

```bash
cd PhotoBlogApp
git init
git add .
git commit -m "feat: initial Android project setup with permissions"
```

---

## Task 6: Android Layouts

**Files:**
- Create: `PhotoBlogApp/app/src/main/res/layout/activity_main.xml`
- Create: `PhotoBlogApp/app/src/main/res/layout/item_image.xml`
- Modify: `PhotoBlogApp/app/src/main/res/values/strings.xml`

**Step 1: Create main activity layout**

Modify: `PhotoBlogApp/app/src/main/res/layout/activity_main.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- Buttons Container -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center"
        android:layout_marginBottom="16dp">

        <Button
            android:id="@+id/btnDownload"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginEnd="8dp"
            android:text="@string/sync_images"
            android:onClick="onClickDownload" />

        <Button
            android:id="@+id/btnUpload"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="8dp"
            android:text="@string/upload_image"
            android:onClick="onClickUpload" />
    </LinearLayout>

    <!-- Status Text -->
    <TextView
        android:id="@+id/tvStatus"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/ready"
        android:textAlignment="center"
        android:textSize="16sp"
        android:layout_marginBottom="16dp"
        android:padding="8dp"
        android:background="@android:color/darker_gray"
        android:textColor="@android:color/white" />

    <!-- Progress Bar -->
    <ProgressBar
        android:id="@+id/progressBar"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        android:visibility="gone" />

    <!-- RecyclerView for Images -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:scrollbars="vertical" />

</LinearLayout>
```

**Step 2: Create image item layout**

Create: `PhotoBlogApp/app/src/main/res/layout/item_image.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardCornerRadius="8dp"
    app:cardElevation="4dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="8dp">

        <ImageView
            android:id="@+id/imageView"
            android:layout_width="match_parent"
            android:layout_height="200dp"
            android:scaleType="centerCrop"
            android:contentDescription="@string/post_image" />

        <TextView
            android:id="@+id/tvTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:textSize="18sp"
            android:textStyle="bold"
            android:text="@string/title_placeholder" />

        <TextView
            android:id="@+id/tvText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textSize="14sp"
            android:text="@string/text_placeholder" />

        <TextView
            android:id="@+id/tvAuthor"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textSize="12sp"
            android:textStyle="italic"
            android:text="@string/author_placeholder" />

    </LinearLayout>

</androidx.cardview.widget.CardView>
```

**Step 3: Update strings.xml**

Modify: `PhotoBlogApp/app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">Photo Blog</string>
    <string name="sync_images">동기화</string>
    <string name="upload_image">새로운 이미지 게시</string>
    <string name="ready">준비됨</string>
    <string name="loading">로딩 중…</string>
    <string name="success">성공!</string>
    <string name="error">오류 발생</string>
    <string name="post_image">게시물 이미지</string>
    <string name="title_placeholder">제목</string>
    <string name="text_placeholder">내용</string>
    <string name="author_placeholder">작성자</string>
</resources>
```

**Step 4: Add CardView dependency**

Modify: `PhotoBlogApp/app/build.gradle` (in dependencies section)

Add:
```gradle
implementation 'androidx.cardview:cardview:1.0.0'
```

**Step 5: Sync Gradle and Preview**

Sync Gradle files
Open activity_main.xml in Design view
Expected: Layout renders correctly with buttons, status text, and RecyclerView

**Step 6: Commit**

```bash
git add app/src/main/res/
git commit -m "feat: add main activity and image item layouts"
```

---

## Task 7: Android ImageAdapter (RecyclerView)

**Files:**
- Create: `PhotoBlogApp/app/src/main/java/com/example/photoblog/ImageAdapter.java`
- Create: `PhotoBlogApp/app/src/main/java/com/example/photoblog/Post.java`

**Step 1: Create Post model class**

Create: `PhotoBlogApp/app/src/main/java/com/example/photoblog/Post.java`

```java
package com.example.photoblog;

import android.graphics.Bitmap;

public class Post {
    private int id;
    private String author;
    private String title;
    private String text;
    private String imageUrl;
    private Bitmap imageBitmap;
    private String createdDate;
    private String publishedDate;

    public Post(int id, String author, String title, String text, String imageUrl) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.text = text;
        this.imageUrl = imageUrl;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Bitmap getImageBitmap() { return imageBitmap; }
    public void setImageBitmap(Bitmap imageBitmap) { this.imageBitmap = imageBitmap; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    public String getPublishedDate() { return publishedDate; }
    public void setPublishedDate(String publishedDate) { this.publishedDate = publishedDate; }
}
```

**Step 2: Create ImageAdapter**

Create: `PhotoBlogApp/app/src/main/java/com/example/photoblog/ImageAdapter.java`

```java
package com.example.photoblog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<Post> postList;

    public ImageAdapter() {
        this.postList = new ArrayList<>();
    }

    public void setPosts(List<Post> posts) {
        this.postList = posts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.tvTitle.setText(post.getTitle());
        holder.tvText.setText(post.getText());
        holder.tvAuthor.setText("By " + post.getAuthor());

        if (post.getImageBitmap() != null) {
            holder.imageView.setImageBitmap(post.getImageBitmap());
        } else {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvTitle;
        TextView tvText;
        TextView tvAuthor;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvText = itemView.findViewById(R.id.tvText);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
        }
    }
}
```

**Step 3: Build project**

In Android Studio: Build → Make Project
Expected: Build successful with no errors

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/photoblog/
git commit -m "feat: add Post model and ImageAdapter for RecyclerView"
```

---

## Task 8: MainActivity Download Implementation

**Files:**
- Modify: `PhotoBlogApp/app/src/main/java/com/example/photoblog/MainActivity.java`

**Step 1: Write MainActivity with download AsyncTask**

Modify: `PhotoBlogApp/app/src/main/java/com/example/photoblog/MainActivity.java`

```java
package com.example.photoblog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String API_URL = "http://10.0.2.2:8000/api_root/Post/";
    private static final String AUTH_TOKEN = "YOUR_TOKEN_HERE"; // Replace with actual token

    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private TextView tvStatus;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter();
        recyclerView.setAdapter(imageAdapter);
    }

    public void onClickDownload(View view) {
        tvStatus.setText("Downloading...");
        progressBar.setVisibility(View.VISIBLE);
        new DownloadImagesTask().execute(API_URL);
    }

    public void onClickUpload(View view) {
        Toast.makeText(this, "Upload feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private class DownloadImagesTask extends AsyncTask<String, Integer, List<Post>> {

        @Override
        protected List<Post> doInBackground(String... urls) {
            List<Post> posts = new ArrayList<>();
            HttpURLConnection connection = null;

            try {
                // Step 1: Get JSON list from API
                URL url = new URL(urls[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Token " + AUTH_TOKEN);
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Log.d(TAG, "Response: " + response.toString());

                    // Parse JSON
                    JSONArray jsonArray = new JSONArray(response.toString());

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonPost = jsonArray.getJSONObject(i);

                        int id = jsonPost.getInt("id");
                        String author = jsonPost.getString("author");
                        String title = jsonPost.getString("title");
                        String text = jsonPost.getString("text");
                        String imageUrl = jsonPost.optString("image", null);

                        Post post = new Post(id, author, title, text, imageUrl);

                        // Step 2: Download image if URL exists
                        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
                            try {
                                Bitmap bitmap = downloadImage(imageUrl);
                                post.setImageBitmap(bitmap);
                            } catch (Exception e) {
                                Log.e(TAG, "Error downloading image: " + imageUrl, e);
                            }
                        }

                        posts.add(post);
                        publishProgress((i + 1) * 100 / jsonArray.length());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error in download task", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return posts;
        }

        private Bitmap downloadImage(String imageUrl) throws Exception {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            connection.disconnect();
            return bitmap;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressBar.setProgress(progress[0]);
            tvStatus.setText("Loading: " + progress[0] + "%");
        }

        @Override
        protected void onPostExecute(List<Post> posts) {
            progressBar.setVisibility(View.GONE);

            if (posts != null && !posts.isEmpty()) {
                imageAdapter.setPosts(posts);
                tvStatus.setText("Loaded " + posts.size() + " posts");
                Toast.makeText(MainActivity.this,
                        "Successfully loaded " + posts.size() + " posts",
                        Toast.LENGTH_SHORT).show();
            } else {
                tvStatus.setText("No posts found");
                Toast.makeText(MainActivity.this,
                        "No posts available",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
```

**Step 2: Update token in MainActivity**

Get token from Django:
```bash
python manage.py shell
```

```python
from rest_framework.authtoken.models import Token
from django.contrib.auth.models import User
user = User.objects.get(username='admin')
token = Token.objects.get(user=user)
print(token.key)
exit()
```

Copy token and replace `YOUR_TOKEN_HERE` in MainActivity.java

**Step 3: Test download functionality**

1. Start Django server: `python manage.py runserver`
2. Run Android app in emulator
3. Click "동기화" button
4. Expected: Images download and display in RecyclerView

**Step 4: Take screenshots**

Required screenshots:
- `docs/screenshots/android-start.png` - App start screen
- `docs/screenshots/android-sync-complete.png` - After sync with images

**Step 5: Commit**

```bash
git add app/src/main/java/com/example/photoblog/MainActivity.java
git add docs/screenshots/
git commit -m "feat: implement image download and display functionality"
```

---

## Task 9: MainActivity Upload Implementation

**Files:**
- Modify: `PhotoBlogApp/app/src/main/java/com/example/photoblog/MainActivity.java`

**Step 1: Add image picker and upload logic**

Modify: `PhotoBlogApp/app/src/main/java/com/example/photoblog/MainActivity.java`

Add constants at top of class:
```java
private static final int PICK_IMAGE_REQUEST = 1;
private android.net.Uri selectedImageUri;
```

Add imports:
```java
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
```

Replace `onClickUpload` method:
```java
public void onClickUpload(View view) {
    Intent intent = new Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    startActivityForResult(intent, PICK_IMAGE_REQUEST);
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
            && data != null && data.getData() != null) {
        selectedImageUri = data.getData();

        // For now, hardcoded upload
        new UploadImageTask().execute(selectedImageUri);
    }
}
```

**Step 2: Add UploadImageTask AsyncTask**

Add at end of MainActivity class:
```java
private class UploadImageTask extends AsyncTask<Uri, Void, Boolean> {

    @Override
    protected void onPreExecute() {
        tvStatus.setText("Uploading...");
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected Boolean doInBackground(Uri... uris) {
        Uri imageUri = uris[0];
        String boundary = "===" + System.currentTimeMillis() + "===";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Authorization", "Token " + AUTH_TOKEN);
            connection.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);

            DataOutputStream outputStream = new DataOutputStream(
                    connection.getOutputStream());

            // Add title field
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"title\"" + lineEnd);
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes("Test Upload from Android" + lineEnd);

            // Add text field
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"text\"" + lineEnd);
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes("This is a test post uploaded from the Android app" + lineEnd);

            // Add image file
            String imagePath = getPathFromUri(imageUri);
            if (imagePath != null) {
                File file = new File(imagePath);
                String fileName = file.getName();

                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"image\";" +
                        " filename=\"" + fileName + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd);
                outputStream.writeBytes(lineEnd);

                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                fileInputStream.close();

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Upload Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_CREATED ||
                        responseCode == HttpURLConnection.HTTP_OK) {
                    return true;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Upload error", e);
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        progressBar.setVisibility(View.GONE);

        if (success) {
            tvStatus.setText("Upload successful!");
            Toast.makeText(MainActivity.this,
                    "Image uploaded successfully",
                    Toast.LENGTH_LONG).show();

            // Refresh list
            new DownloadImagesTask().execute(API_URL);
        } else {
            tvStatus.setText("Upload failed");
            Toast.makeText(MainActivity.this,
                    "Failed to upload image",
                    Toast.LENGTH_LONG).show();
        }
    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        android.database.Cursor cursor = getContentResolver()
                .query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
        return null;
    }
}
```

**Step 3: Test upload functionality**

1. Start Django server
2. Run Android app
3. Click "새로운 이미지 게시"
4. Select an image
5. Expected: Upload success message, image appears in list

**Step 4: Verify in Django admin**

Open http://127.0.0.1:8000/admin/blog/post/
Expected: New post visible with uploaded image

**Step 5: Take screenshot**

Screenshot: `docs/screenshots/android-upload-success.png`

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/photoblog/MainActivity.java
git add docs/screenshots/
git commit -m "feat: implement image upload with multipart form-data"
```

---

## Task 10: PythonAnywhere Deployment

**Files:**
- Create: `docs/deployment.md`
- Modify: `mysite/settings.py`

**Step 1: Prepare for deployment**

Update: `mysite/settings.py`

Change:
```python
DEBUG = False

ALLOWED_HOSTS = ['your-username.pythonanywhere.com', '127.0.0.1', 'localhost', '10.0.2.2']
```

**Step 2: Create deployment documentation**

Create: `docs/deployment.md`

```markdown
# PythonAnywhere Deployment Guide

## Step 1: Create PythonAnywhere Account

1. Go to https://www.pythonanywhere.com
2. Sign up for free account
3. Confirm email

## Step 2: Upload Code

### Option A: Git Clone
```bash
cd ~
git clone YOUR_GITHUB_REPO_URL photoblog
cd photoblog
```

### Option B: Upload Files
Use PythonAnywhere file upload interface

## Step 3: Create Virtual Environment

```bash
cd ~/photoblog
python3.10 -m venv djangoenv
source djangoenv/bin/activate
pip install -r requirements.txt
```

## Step 4: Configure Database

```bash
python manage.py migrate
python manage.py createsuperuser
# Create admin user
```

## Step 5: Collect Static Files

```bash
python manage.py collectstatic --noinput
```

## Step 6: Configure Web App

1. Go to Web tab
2. Add a new web app
3. Select "Manual configuration"
4. Select Python 3.10
5. Set:
   - Source code: /home/username/photoblog
   - Working directory: /home/username/photoblog
   - Virtualenv: /home/username/photoblog/djangoenv

## Step 7: Configure WSGI File

Edit /var/www/username_pythonanywhere_com_wsgi.py:

```python
import os
import sys

path = '/home/username/photoblog'
if path not in sys.path:
    sys.path.append(path)

os.environ['DJANGO_SETTINGS_MODULE'] = 'mysite.settings'

from django.core.wsgi import get_wsgi_application
application = get_wsgi_application()
```

## Step 8: Configure Static/Media Files

In Web tab, add static files mappings:
- URL: /static/ → Directory: /home/username/photoblog/static
- URL: /media/ → Directory: /home/username/photoblog/media

## Step 9: Reload Web App

Click "Reload" button in Web tab

## Step 10: Test

Visit: https://username.pythonanywhere.com/api_root/Post/

Expected: API returns JSON list

## Step 11: Create Token

```bash
python manage.py shell
```

```python
from django.contrib.auth.models import User
from rest_framework.authtoken.models import Token
user = User.objects.get(username='admin')
token, created = Token.objects.get_or_create(user=user)
print(f"Token: {token.key}")
exit()
```

## Step 12: Update Android App

Change API_URL in MainActivity.java:
```java
private static final String API_URL = "https://username.pythonanywhere.com/api_root/Post/";
private static final String AUTH_TOKEN = "your_new_token";
```

## Step 13: Update Network Security Config

Modify network_security_config.xml:
```xml
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">username.pythonanywhere.com</domain>
</domain-config>
```

Note: HTTPS doesn't need cleartext permission
```

**Step 3: Deploy to PythonAnywhere**

Follow steps in docs/deployment.md
Expected: App accessible at https://username.pythonanywhere.com

**Step 4: Test deployed API**

```bash
curl -H "Authorization: Token YOUR_TOKEN" \
     https://username.pythonanywhere.com/api_root/Post/
```

Expected: JSON response with posts

**Step 5: Update Android app with production URL**

Update MainActivity.java with PythonAnywhere URL
Test app with production server
Expected: App works with deployed server

**Step 6: Take screenshots**

Required:
- `docs/screenshots/pythonanywhere-api.png` - Browser showing API
- `docs/screenshots/pythonanywhere-admin.png` - Admin interface
- `docs/screenshots/android-production.png` - App working with production server

**Step 7: Commit**

```bash
git add mysite/settings.py docs/deployment.md docs/screenshots/
git commit -m "feat: add PythonAnywhere deployment configuration and docs"
```

---

## Task 11: Documentation and Submission

**Files:**
- Create: `README.md`
- Create: `docs/submission-checklist.md`
- Update: `docs/api-verification.md`

**Step 1: Create comprehensive README**

Create: `README.md`

```markdown
# Photo Blog Mobile/Web Service

Django REST Framework backend with Android client for photo blog service.

## Features

- RESTful API for photo posts
- Token-based authentication
- Image upload with multipart/form-data
- Android client with sync and upload
- PythonAnywhere deployment

## Project Structure

```
mwp-midterm-blog/
├── mysite/              # Django project settings
├── blog/                # Blog app (models, views, serializers)
├── media/               # Uploaded images
├── docs/                # Documentation and screenshots
│   ├── plans/           # Implementation plans
│   ├── screenshots/     # Required screenshots
│   ├── api-verification.md
│   └── deployment.md
├── PhotoBlogApp/        # Android client
│   └── app/src/main/
│       ├── java/        # Java source
│       └── res/         # Android resources
├── requirements.txt
└── README.md
```

## Server Setup

### Local Development

```bash
# Create virtual environment
python -m venv djangoenv
source djangoenv/bin/activate  # Windows: djangoenv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run migrations
python manage.py migrate

# Create superuser
python manage.py createsuperuser

# Run server
python manage.py runserver
```

### Get Authentication Token

```bash
python manage.py shell
```

```python
from rest_framework.authtoken.models import Token
from django.contrib.auth.models import User
user = User.objects.get(username='admin')
token = Token.objects.get_or_create(user=user)[0]
print(f"Token: {token.key}")
```

## API Endpoints

### GET /api_root/Post/

List all posts

```bash
curl -H "Authorization: Token YOUR_TOKEN" \
     http://127.0.0.1:8000/api_root/Post/
```

Response:
```json
[
  {
    "id": 1,
    "author": "admin",
    "title": "Test Post",
    "text": "Test content",
    "created_date": "2025-11-02T10:00:00Z",
    "published_date": "2025-11-02T10:00:00Z",
    "image": "http://127.0.0.1:8000/media/images/test.jpg"
  }
]
```

### POST /api_root/Post/

Create new post with image

```bash
curl -X POST \
     -H "Authorization: Token YOUR_TOKEN" \
     -F "title=New Post" \
     -F "text=Post content" \
     -F "image=@/path/to/image.jpg" \
     http://127.0.0.1:8000/api_root/Post/
```

## Android Client

### Configuration

Update in `MainActivity.java`:

```java
private static final String API_URL = "http://10.0.2.2:8000/api_root/Post/";
private static final String AUTH_TOKEN = "your_token_here";
```

Note: Use `10.0.2.2` for Android emulator to access localhost

### Build and Run

1. Open PhotoBlogApp in Android Studio
2. Sync Gradle
3. Run on emulator or device
4. Click "동기화" to download images
5. Click "새로운 이미지 게시" to upload

## Testing

### API Verification with Telnet

See `docs/api-verification.md` for detailed steps.

```bash
telnet 127.0.0.1 8000
```

```
GET /api_root/Post/ HTTP/1.1
Host: 127.0.0.1:8000
Authorization: Token YOUR_TOKEN

```

### JSON Validation

Copy JSON response and validate at:
https://codebeautify.org/jsonviewer

## Deployment

See `docs/deployment.md` for PythonAnywhere deployment guide.

## Screenshots

Required screenshots in `docs/screenshots/`:

1. `telnet-request.png` - Telnet API request/response
2. `json-validation.png` - JSON validator result
3. `browser-api.png` - Browser showing API response
4. `android-start.png` - App start screen
5. `android-sync-complete.png` - After sync with images
6. `android-upload-success.png` - After successful upload
7. `pythonanywhere-api.png` - Production API
8. `pythonanywhere-admin.png` - Production admin
9. `android-production.png` - App with production server

## Tech Stack

- **Backend:** Django 3.2, Django REST Framework 3.14
- **Frontend:** Android (Java), RecyclerView
- **Database:** SQLite (development), can use PostgreSQL (production)
- **Authentication:** Token-based
- **Deployment:** PythonAnywhere

## License

Educational project for mobile/web services course.
```

**Step 2: Create submission checklist**

Create: `docs/submission-checklist.md`

```markdown
# Submission Checklist

## Code Repositories

- [ ] Django server code pushed to GitHub
- [ ] Android client code pushed to GitHub
- [ ] Both repos have clear README.md
- [ ] .gitignore configured (no secrets, media files, etc.)

## Server Implementation

- [ ] Django project with REST Framework
- [ ] Post model with image field
- [ ] Token authentication configured
- [ ] GET /api_root/Post/ returns JSON array
- [ ] POST /api_root/Post/ accepts multipart upload
- [ ] Absolute URLs for images in response
- [ ] Admin interface accessible

## Android Client Implementation

- [ ] Permissions in AndroidManifest.xml
- [ ] Network security config for HTTP
- [ ] RecyclerView with ImageAdapter
- [ ] Download functionality working (동기화)
- [ ] Upload functionality working (새로운 이미지 게시)
- [ ] Progress indication during operations
- [ ] Error handling with Toast messages

## API Verification (평가 항목 2,3)

- [ ] Telnet request/response captured
- [ ] Screenshot of telnet session
- [ ] JSON validated at codebeautify.org
- [ ] Screenshot of JSON validation
- [ ] Both screenshots in docs/screenshots/

## Server/Client Integration Testing

- [ ] Browser screenshot of API response
- [ ] Android start screen screenshot
- [ ] Android sync complete screenshot
- [ ] All three screenshots in docs/screenshots/

## Upload Functionality

- [ ] MainActivity upload code implemented (not commented out)
- [ ] Upload tested and working
- [ ] Screenshot of successful upload
- [ ] Uploaded post visible in admin

## PythonAnywhere Deployment

- [ ] App deployed to PythonAnywhere
- [ ] API accessible via public URL
- [ ] Media files serving correctly
- [ ] Screenshot of production API
- [ ] Screenshot of production admin
- [ ] Android app tested with production URL
- [ ] Screenshot of app with production server

## Additional Features (Optional, max 5)

- [ ] Feature 1: _______________
- [ ] Feature 2: _______________
- [ ] Feature 3: _______________
- [ ] Feature 4: _______________
- [ ] Feature 5: _______________

Each with description and screenshot

## Documentation

- [ ] README.md with setup instructions
- [ ] API documentation
- [ ] Deployment guide
- [ ] All required screenshots collected
- [ ] MS Word report prepared with all screenshots

## Final Checks

- [ ] All code compiles/runs without errors
- [ ] No hardcoded passwords in committed code
- [ ] requirements.txt is up to date
- [ ] Android app APK builds successfully
- [ ] Both local and production environments tested
- [ ] GitHub repository URLs ready for submission

## Report Structure (MS Word)

Following template: 모바일/웹서비스 프로젝트 공통평가 01_수행 결과 보고서.docx

- [ ] Project overview
- [ ] Architecture diagram
- [ ] API specification
- [ ] Implementation details
- [ ] All required screenshots embedded
- [ ] Testing results
- [ ] Deployment information
- [ ] Conclusion

## Submission Package

- [ ] MS Word report (PDF backup)
- [ ] GitHub URLs document
- [ ] All screenshots organized
- [ ] APK file (optional)
```

**Step 3: Verify all screenshots**

Check that all required screenshots exist:
```bash
ls -la docs/screenshots/
```

Expected files:
- telnet-request.png
- json-validation.png
- browser-api.png
- android-start.png
- android-sync-complete.png
- android-upload-success.png
- pythonanywhere-api.png
- pythonanywhere-admin.png
- android-production.png

**Step 4: Final commit**

```bash
git add README.md docs/
git commit -m "docs: add comprehensive documentation and submission checklist"
```

**Step 5: Push to GitHub**

```bash
git remote add origin YOUR_GITHUB_REPO_URL
git branch -M main
git push -u origin main
```

**Step 6: Verify GitHub repository**

Visit your GitHub repo
Expected: All files visible, README renders correctly

---

## Execution Complete

All tasks in the implementation plan are now defined with:

- ✅ Exact file paths for every file to create/modify
- ✅ Complete code examples (no placeholders like "add validation")
- ✅ Specific commands with expected outputs
- ✅ Step-by-step verification procedures
- ✅ Commit messages following conventional commits
- ✅ Screenshots documentation
- ✅ Deployment procedures
- ✅ Submission checklist

## Next Steps

Execute this plan using one of two approaches:

**Option 1: Sequential Execution (Recommended for Learning)**
- Follow tasks 1-11 in order
- Test each task before moving to next
- Take screenshots as you go
- Commit frequently

**Option 2: Batch Execution with Checkpoints**
- Execute tasks in batches (e.g., 1-3, 4-6, 7-9, 10-11)
- Review and test after each batch
- Fix issues before proceeding

Both approaches follow TDD, DRY, YAGNI principles with frequent commits.
