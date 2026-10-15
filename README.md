# GORAYA Construction Khata

اردو، آف لائن تعمیراتی کھاتہ (Kotlin + Jetpack Compose + Room)۔

## APK بنانے کا طریقہ
1. تمام فائلیں اس ریپوزٹری میں اپ لوڈ/کمٹ کریں۔
2. GitHub پر **Actions** ٹیب کھولیں ← **Build Debug APK** (خود چلتا ہے، یا Run workflow دبائیں)۔
3. بلڈ مکمل ہو تو اس رن کے صفحے کے نیچے **Artifacts** میں `GORAYA-Khata-debug-apk` ڈاؤن لوڈ کریں۔
4. زپ کھولیں، موبائل میں APK انسٹال کریں (انجان ذرائع کی اجازت دینی ہوگی)۔

## Debug اور Release میں فرق
- Debug: ٹیسٹنگ کے لیے، خودکار سائن، بغیر سیٹ اپ کے بنتی ہے۔
- Release: اپنی keystore سے سائن ہوتی ہے۔ GitHub Secrets میں `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` رکھیں، پھر **Build Release APK (signed)** چلائیں۔ keystore کبھی ریپوزٹری میں اپ لوڈ نہ کریں۔

## موجودہ حالت
بیک اَپ اور بحالی (مقامی اور آن لائن) ابھی شامل نہیں۔ یہ اگلا مرحلہ ہے۔
