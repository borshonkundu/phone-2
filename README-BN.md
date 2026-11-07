# PasteDialer 2.3 — Android Phone/Dialer source

এই প্রজেক্টটি হালকা Samsung One UI-inspired dialer prototype।

## Android 5.1.1 সীমাবদ্ধতা
Android 5.1.1-এ সিস্টেমের পূর্ণ Default Dialer role, `InCallService` এবং public in-call DTMF API নেই। তাই এই OS-এ চলমান কলের আসল keypad-এর মধ্যে paste করে স্বয়ংক্রিয়ভাবে DTMF পাঠানো যাবে না। এই সোর্সে floating overlay panel-ও এখনো সম্পূর্ণ implement করা হয়নি।

## Call recording ও SD card
কোডে VOICE_CALL audio source চেষ্টা করা হয় এবং ব্যর্থ হলে MIC source ব্যবহার করা হয়। অনেক ফোন/অপারেটর call audio capture বন্ধ রাখে; MIC fallback অপর পক্ষের কণ্ঠ ধারণ নাও করতে পারে। SD folder picker UI সম্পূর্ণ যুক্ত নয়, তাই SD card-এ রেকর্ডিং নিশ্চিত নয়। রেকর্ডিংয়ের আগে স্থানীয় আইন ও সম্মতি মেনে চলুন।

## Build
Repository root-এ `.github/workflows/build.yml` আছে। GitHub Actions থেকে `Build PasteDialer APK` workflow চালিয়ে build চেষ্টা করা যাবে। Build সফল হওয়া মানেই বাস্তব কল/রেকর্ডিং ফিচার পরীক্ষা সফল—এমন নয়।

## Configuration
- minSdk 21
- targetSdk 28
- compileSdk 34
- Android Gradle Plugin 8.5.2
- Gradle 8.7
- Java 17

বিস্তারিত বাস্তব সীমাবদ্ধতা `FINAL-STATUS-BN.md`-এ দেখুন।
