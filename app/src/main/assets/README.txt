IMPORTANT: MediaPipe Model File Required
========================================

Download pose_landmarker_full.task and place it in this folder (app/src/main/assets/).

Download link:
https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/latest/pose_landmarker_full.task

Steps:
1. Open the URL above in your browser
2. The file will download automatically (~29 MB)
3. Copy it into this folder: app/src/main/assets/
4. Rebuild the project

Without this file, the camera/pose features will show a warning banner
but the rest of the app (progress, avatar, history) will work normally.
