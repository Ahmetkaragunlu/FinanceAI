# Dış servis yapılandırması

## Yerel kurulum ve CI

Gerçek Firebase yapılandırmaları, Maps anahtarı ve App Check debug tokenleri Git dışında tutulur. Projeyi klonlayan geliştirici kendi Firebase projelerini ve Maps anahtarını yapılandırmalıdır.

1. Ana Firebase projesinde `com.ahmetkaragunlu.financeai` paket adıyla Android uygulamasını kaydedin. Firebase Authentication Google sağlayıcısını etkinleştirin; kendi debug/yayın sertifikalarınızın SHA değerlerini ekleyin. Console'dan indirilen gerçek `google-services.json` dosyasını `app/google-services.json` konumuna koyun.
2. `app/google-services.json.example` yalnız dosya yapısını gösterir. Sahte değerleri ve boş OAuth listesiyle çalışan bir yapılandırma değildir. Google girişi için Console'dan indirilen dosyada web OAuth client kaydı bulunmalıdır.
3. AI erişimi için ayrı Firebase projesinde aynı paket adıyla Android uygulamasını kaydedin; Firebase AI Logic üzerinden Gemini Developer API erişimini yapılandırın. Bu projenin Android istemci yapılandırmasındaki `project_id`, `mobilesdk_app_id` ve `current_key` değerlerini aşağıdaki üç `AI_FIREBASE_*` alanına aktarın. AI JSON'unu `res/raw` içine koymayın.
4. `local.properties.example` alanlarını ignore edilen `local.properties` dosyasına doldurun:

```properties
sdk.dir=/path/to/Android/sdk
MAPS_API_KEY=your-restricted-android-maps-key
AI_FIREBASE_PROJECT_ID=your-ai-firebase-project-id
AI_FIREBASE_APP_ID=your-ai-firebase-android-app-id
AI_FIREBASE_API_KEY=your-restricted-ai-firebase-client-key
```

Maps ve AI alanları environment → Gradle property → `local.properties` önceliğiyle okunur. Eksik alanın adı Android Studio sync/Gradle yapılandırması sırasında hata mesajında gösterilir. CI aynı alanları güvenli değişkenlerden sağlar ve ana Firebase JSON'unu Git dışındaki CI yapılandırmasından `app/google-services.json` konumuna yerleştirir. Gerçek değerler build loglarına veya artifact olarak yayımlanan kaynak dosyalarına yazılmamalıdır.

Anahtar değişikliğinden sonra temiz derleme alın. Kotlin'in BuildConfig sabitlerini kullanan sınıflarında artımlı derleme eski değerleri tutabilir:

```bash
bash ./gradlew clean assembleDebug
```

## Firebase sahipliği

Ana `[DEFAULT]` FirebaseApp, Authentication, Firestore, Storage, FCM ve Functions istemcilerinin sahibidir. Kullanıcı verileri, finansal kayıtlar ve sohbet geçmişi ana projede kalır. Google Services plugin'i yerel ana JSON'dan gerekli Android kaynaklarını ve OAuth web client ID'sini üretir.

Yalnız AI model erişimi `finance-ai` adlı ikinci FirebaseApp'e bağlanır. Demo için ayrı Spark projesi kullanılmasının nedeni AI erişimini ücretsiz kota kapsamında tutmaktır; erişim sağlayıcı koşullarına ve kotaya tabidir. `@AiFirebaseApp` qualifier bu istemcinin bağımlılıklarını ayırır. `AiFirebaseConfig.of(...)`, `BuildConfig.AI_FIREBASE_*` alanlarından FirebaseOptions oluşturur. İkinci Auth/veritabanı/FCM istemcisi kurulmaz.

Android'de ayrıca bir `GEMINI_API_KEY` gerekmez. Firebase AI Logic'in sunucu tarafındaki Gemini Developer API anahtarı Android kaynaklarına, BuildConfig'e veya Git'e alınmamalıdır.

## Anahtar kısıtları ve App Check

Firebase ve Maps istemci anahtarları APK içinde bulunabilir. Git dışında tutulmaları repo hijyeni sağlar; veri erişimi Security Rules, Authentication ve uygun App Check doğrulamasıyla korunur. Firebase istemci anahtarlarının API izinleri gerekli Firebase servisleriyle sınırlı olmalıdır; `generativelanguage.googleapis.com` istemci anahtarının izin listesine eklenmemelidir. Maps anahtarı uygun Android paket/sertifika ve kullanılan Maps API'leriyle kısıtlanmalıdır. Firebase istemci anahtarlarına uygulanacak uygulama kısıtları SDK uyumluluğu doğrulanarak seçilmelidir.

Debug variant Debug App Check provider, release variant Play Integrity provider kullanır. App Check iki FirebaseApp için ayrı kurulur. Geliştirme kurulumunun debug tokenini her iki projenin Android uygulamasında **App Check → Apps → üç nokta → Manage debug tokens** yoluyla kaydedin. Token dosyaları `appcheck-debug-token.local.txt` ve `appcheck-ai-debug-token.local.txt` yereldir; Git/BuildConfig içine alınmaz. Uygulamanın verileri silindiğinde veya kurulum yenilendiğinde token değişebilir.

Yayın için release imzalama ve iki Firebase/Google OAuth/Maps tarafındaki uygun yayın sertifikaları ayrıca hazırlanmalıdır. SHA parmak izi herkese açık sertifika bilgisidir; keystore/private key değildir. Functions deploy için geliştiricinin kendi Firebase CLI hesabı ve proje erişimi kullanılır; servis hesabı private key'i Android'e eklenmez.

## AI davranışı

Model `gemini-3.5-flash-lite`; mevcut LOW thinking, timeout ve retry ayarları korunur. Modele aktif hesabın içinde bulunulan takvim ayındaki finansal raporu gönderilir. Ay/yıl ve ISO tarih sınırları dil bağımsızdır; cevap dili hızlı XML sorusu veya manuel soru metninden belirlenir. Tutar ve para birimi korunur. Sohbet geçmişinin saklanması ve ana Firebase veri sahipliği bu yapılandırma değişikliğinden etkilenmez.

## Git ve anahtar yenileme

`app/google-services.json`, eski `app/src/main/res/raw/ai_google_services.json`, `local.properties`, `.env`, keystore/private key ve App Check debug token dosyaları ignore edilir. Takip edilmiş dosyaları ignore etmek geçmişi temizlemez; `git rm --cached` ile takipten çıkarmak ve geçmişteki gerçek değerleri ayrıca temizlemek gerekir.

Anahtar yenileme mevcut Firebase proje/Android app kimliklerini korumalıdır. Yeni kısıtlı anahtarlar yerel yapılandırmaya aktarıldıktan ve doğrulandıktan sonra eski anahtarlar iptal edilir. Geçmiş temizliği branch/tag commit kimliklerini değiştirir; GitHub'a güncelleme ve varsa eski PR/cache kayıtları için ek GitHub işlemleri gerekebilir. Eski faz/ilerleme belgeleri tarihçe olarak korunur.

Kaynaklar: [Firebase API anahtarları](https://firebase.google.com/docs/projects/api-keys), [App Check debug provider](https://firebase.google.com/docs/app-check/android/debug-provider), [Firebase AI Logic](https://firebase.google.com/docs/ai-logic).
