# PasteDialer 2.3 — QA রিপোর্ট

- XML resource ও AndroidManifest XML parser দিয়ে যাচাই করা হয়েছে।
- Gradle configuration-এ targetSdk 28, minSdk 21, compileSdk 34 রাখা হয়েছে।
- GitHub Actions workflow উপস্থিত আছে।
- এই পরিবেশে Android SDK/Gradle build চালানো হয়নি; তাই APK compile success দাবি করা হচ্ছে না।
- Samsung J2 Android 5.1.1-এ ইনস্টল/কল/SD recording পরীক্ষা করা হয়নি।

## গুরুত্বপূর্ণ সীমাবদ্ধতা
Android 5.1.1 (API 22)-এ InCallService এবং public in-call DTMF API নেই। ফলে active-call keypad-এ paste করে স্বয়ংক্রিয় DTMF পাঠানো সম্ভব নয়। Floating overlay panel-ও এই সোর্সে এখনো implement করা হয়নি।

Call recording device/firmware/operator restrictions-এর কারণে ব্লক হতে পারে। VOICE_CALL capture অনেক ডিভাইসে অনুমোদিত নয়; MIC fallback-এ অপর পক্ষের কণ্ঠ রেকর্ড নাও হতে পারে। SD folder picker UI এই সোর্সে সম্পূর্ণ যুক্ত নয়, তাই SD card recording এখনো সম্পূর্ণ/পরীক্ষিত ফিচার নয়।
