# PasteDialer 2.3 — বাস্তব অবস্থা ও সীমাবদ্ধতা

এই ZIP একটি Android source project; এটি তৈরি APK নয়। এই পরিবেশে Android SDK/Gradle দিয়ে APK compile করা বা Samsung J2-তে চালিয়ে পরীক্ষা করা হয়নি। তাই একে পরীক্ষিত/bug-free final release বলা যাবে না।

## Android 5.1.1 (API 22)
- সাধারণ ডায়ালার UI, নম্বর পেস্ট, contacts/recents এবং ACTION_CALL/ACTION_DIAL flow প্রজেক্টে আছে।
- Android 5.1.1-এ `InCallService` এবং `Call.playDtmfTone()` public API নেই। তাই সিস্টেমের আসল active-call keypad-এ স্বয়ংক্রিয় DTMF পাঠানো বা আসল in-call screen বদলানো যাবে না।
- এই সোর্সে Android 5.1.1-এর জন্য floating overlay panel এখনো বাস্তবায়িত নয়। তাই active call চলাকালে অন্য অ্যাপের ওপর নম্বর-প্যানেল দেখাবে—এমন নিশ্চয়তা নেই।

## Call recording / SD card
- `CallRecorder`-এ `VOICE_CALL` source চেষ্টা করে, ব্যর্থ হলে `MIC` source চেষ্টা করা হয়। Android/ফোন/অপারেটর অনেক সময় call audio capture ব্লক করে; MIC fallback-এ অপর পক্ষের কণ্ঠ নাও রেকর্ড হতে পারে।
- SD card-এ রেকর্ড সেভ করার কোডে SAF folder URI দরকার, কিন্তু এই প্রজেক্টে SD folder বেছে নেওয়ার Settings/Picker UI সম্পূর্ণভাবে যুক্ত নেই।
- SD card-এ নির্ভরযোগ্য রেকর্ডিং এবং দুই পাশের কণ্ঠ ধারণ Samsung J2-তে বাস্তব পরীক্ষা ছাড়া নিশ্চিত করা যায় না।
- কল রেকর্ড করার আগে স্থানীয় আইন ও অপর পক্ষের সম্মতির নিয়ম মেনে চলুন।

## Build configuration
- compileSdk 34, minSdk 21, targetSdk 28, Android Gradle Plugin 8.5.2, Gradle 8.7, Java 17।
- GitHub Actions workflow: `.github/workflows/build.yml`।

## কী যাচাই হয়েছে
- XML resource ও Manifest XML parse করা হয়েছে।
- ZIP-এর file structure যাচাই করা হয়েছে।
- APK compilation এবং real-device tests করা হয়নি। GitHub Actions build সফল হলেও সেটি শুধু compile প্রমাণ করে; কল, DTMF বা recording সঠিকভাবে চলে—তা প্রমাণ করে না।
