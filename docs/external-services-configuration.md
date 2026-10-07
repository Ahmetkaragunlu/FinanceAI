# Dış servis yapılandırması

**Yanıt dili:** Hızlı XML sorusunun veya manuel soru metninin dili esas alınır. Cihaz locale'inden üretilen ay metni kaldırıldı; periodYear/periodMonth ve ISO tarih sınırları dil bağımsızdır. System instruction tüm cevabı ve ay/kategori/tür adlarını soru diline uyarlarken tutar/currency değerlerini korur. XML'deki Türkçe soruda Ekim, İngilizce manuel soruda October hedeflenir; canlı yanıtı kullanıcı doğrular. İkinci projenin gerekçesi AiFirebaseApp KDoc içinde İngilizce belgelenmiştir; bağlantı/anahtar/token/Console ayarı değişmemiştir.

**Güncel AI veri kapsamı:** Kullanıcının onaylı son kararıyla yalnız içinde bulunulan takvim ayının aktif hesap işlemleri gönderilir. Gelir/gider/bakiye, bütçe kullanımı ve kategori sıralaması aynı ay sınırında uygulamada hesaplanır; dönem/boş kayıt/yalnız gelir/bütçe eksikliği açık JSON alanlarıdır. Eski kayıtlar ve sohbet saklanır, modele gönderilmez. System instruction selamlama/kimlik/finans dışı/belirsiz soruları ve veri yokluğunu ayrı ele alır. Aşağıdaki tüm geçmiş kapsamı ifadeleri önceki kararın tarihçesidir.

**Güncel model:** Kullanıcı onayıyla `gemini-3.5-flash-lite`; yalnız model adı değişti. Mevcut LOW thinking, timeout ve retry ayarları korundu. Aynı `financeai-ai` projesinde doğrulanan kota tanımı 15 istek/dakika ve 500 istek/gün. Yeni JSON, key, token veya Console değişikliği gerekmedi; gerçek yanıtı kullanıcı uygulamadan kontrol edecek. Aşağıdaki 3.8 ifadeleri önceki uygulama kayıtlarıdır.

**AI gecikme düzeltmesi:** `firebase-ai` catalog override 17.17.0, aynı `gemini-3.8-flash` için `ThinkingLevel.LOW`, request timeout25s ve en fazla iki model denemesi / toplam model bütçesi55s. AI App Check preflight en fazla8s; access/network/timeout/server hata mesajları ayrıdır. Secret yenilenmesi sonrası kullanıcı her iki yeni tokeni kaydetti, AI exchange HTTP200 ile doğrulandı. Agent yeni canlı model çağrısı göndermedi; son yanıt/süre kontrolünü kullanıcı uygulamadan yapar. Diğer Firebase sürümlerinin genel catalog modernizasyonu Faz5'e ait kalır; gerekli AI transitive dependency uyumu derlemeyle doğrulandı.

**7 Ekim son canlı durum:** Yeni AI projesinin debug tokeni kullanıcı tarafından kaydedildi; gerçek SDK App Check exchange başarılı. `gemini-3.8-flash` çağrısında ilk deneme zaman aşımı, ikinci deneme sağlayıcı yoğun talep hatasıyla sonuçlandı. Yeni projeye erişim doğrulandı, başarılı AI cevabı doğrulanmadı. Yeni token/billing ayarı gerekmediği bu kontrolde görüldü; başka model veya otomatik fallback eklenmedi.

Bu dosya kurulum ayrıntılarını tutar; nihai README refaktör tamamlanınca hazırlanır.

## Android yerel ve CI yapılandırması

`local.properties.example` içindeki `sdk.dir` ve `MAPS_API_KEY` girdilerini ignore edilen `local.properties` dosyasına koyun. CI için `MAPS_API_KEY` ortam değişkeni veya Gradle property kullanılabilir. Öncelik environment → Gradle property → local.properties şeklindedir. Anahtar Manifest placeholder aracılığıyla Maps SDK'ya verilir; APK'daki istemci anahtarı gizli sayılmaz, uygulama/API kısıtlarıyla korunur.

`google-services.json` mevcut FinanceAI Android istemci yapılandırmasıdır. Service-account/private key değildir. Firebase anahtarı gereken Firebase API'lerine kısıtlanmalıdır. Private key, keystore, `.env`, parola ve App Check debug secret dosyaları Git'e gönderilmez.

Firebase AI Logic, Firebase Android BoM ile yönetilen `firebase-ai` SDK'sını ve Gemini Developer API sağlayıcısını kullanır. Android'de `GEMINI_API_KEY` veya Gemini BuildConfig alanı gerekmez. Model erişimi `finance-ai` adlı FirebaseApp üzerinden ayrı `financeai-ai` projesine bağlanır. `@AiFirebaseApp` qualifier yalnız AI provider'ına aittir; Auth, Firestore, Storage, FCM ve Functions ana `financeai-7e7bc` projesinin default istemcilerini kullanır. Sohbet geçmişi ve finansal kayıtlar ana projede kalır.

Ana Google Services plugin'i `app/google-services.json` dosyasını işlemeye devam eder. İkinci Android istemci dosyası, kullanıcının son `google-services (2).json` dosyasından `app/src/main/res/raw/ai_google_services.json` kaynağına alınır; `AiFirebaseConfig` doğru Android paketini doğrulayıp named app için gereken FirebaseOptions'ı oluşturur. Bunlar Firebase istemci yapılandırmalarıdır; service-account veya Gemini gizli anahtarı içermez.

## Credential Manager

Google düğmesi explicit `GetSignInWithGoogleOption` akışını başlatır. OAuth web client ID, Google Services plugin'in ürettiği `default_web_client_id` kaynağından alınır. Android paket/imza kaydı kullanılan debug/upload/Play sertifikasıyla eşleşmelidir. Firebase Google provider mevcut kayıtlı hesap politikasıyla çalışır; bilinmeyen hesap için otomatik kayıt yapılmaz. Cihaz provider seçicisinin görünümü Android/Google tarafından yönetilir; uygulamanın düğme/layout'u korunur.

## App Check ve Console

Debug variant Debug App Check provider, release variant Play Integrity provider kullanır. App Check her FirebaseApp için ayrı kurulur. Her yeni debug kurulumu farklı bir debug secret oluşturabilir; tekrar kurulan emülatörün tokeni mevcut Console kaydıyla aynı varsayılmaz. Tokeni ilgili projenin Android app App Check allowlist'ine kaydedin. Ana proje için `appcheck-debug-token.local.txt`, AI projesi için `appcheck-ai-debug-token.local.txt` yalnız yerel geliştirme içindir; ignore edilir, AI dosyasının izni `600` ve değerler APK/BuildConfig'e gömülmez. Yeni AI tokeninin kayıt yolu: `financeai-ai → App Check → Apps → FinanceAI-AI → üç nokta → Manage debug tokens → Add debug token`. Kullanıcı kaydı tamamladığını söyleyene kadar canlı AI çağrısı yapılmaz.

7 Ekim 2026 salt-okuma Console kontrolü: Maps anahtarı FinanceAI Android paketine ve Maps Android/Places/Geocoding servislerine kısıtlı; kayıtlı eski SHA1 mevcut Mac debug imzasıyla eşleşmiyor. Firebase istemci anahtarında gerekli Firebase/App Check/AI API hedefleri mevcut. AI Logic `templateOnly=false`, `firebaseAuthRequired=false`; bu ayarlar değiştirilmedi. App Check servis kaydında AI için `firebaseml.googleapis.com` enforcement aktif, Firestore/Storage enforcement kapalı. Firestore/Storage kullanıcı erişimi Rules ile kontrol edilir; App Check ek katmandır.

API kısıtını değiştirmek, yeni billing/model politikası seçmek, token eklemek veya anahtarı yenilemek kullanıcı kararıyla yapılır. SHA parmak izi public sertifika bilgisidir; imzalama özel anahtarı değildir. APK yayımlanmadığı için release signing/R8 kurulumu eklenmedi.

Kullanıcı onayıyla 7 Ekim'de yalnız mevcut emülatörün debug tokeni eklendi (1→2, önceki kayıt korundu) ve Maps anahtarına Mac debug sertifikası eklendi (1→2, eski sertifika/API hedefleri korundu). Mac sertifikası `F6:EE:DA:38:5E:E7:D4:03:D6:82:94:8F:0F:65:CA:F8:EE:3B:8C:0D`. Google Services JSON içinde aynı Android OAuth sertifika eşleşmesi ve web client ID mevcut. Son App Check canlı doğrulaması başarılı; sonraki model aşaması ödeme kredisine takıldı.

## Model, maliyet ve veri

Kullanıcı onayıyla model `gemini-3.8-flash` olarak güncellendi. Firebase'in güncel desteklenen model listesinde bu model billing gerektirmeyen seçenek olarak bulunur; gerçek proje erişimi ve yanıt başarısı token kaydı sonrasında canlı sentetik kontrolde doğrulanır. Sessiz fallback veya farklı modele otomatik geçiş yapılmaz. Kaynak: https://firebase.google.com/docs/ai-logic/models

FinanceAI Functions için Blaze'a geçti. Billing bağlı projede Gemini Developer API kullanımı kullanım bazlı ücretlenir; Gemini için Spark ücretsiz kota varsayımı geçerli değildir. Kullanıcı düşük hacimli canlı kullanımı ayrıca kabul etti. İstek gönderilmiyorsa AI kullanım ücreti oluşmaz; diğer Firebase servislerinin kendi ücretsiz kotaları ve zamanlanmış görevleri ayrıdır. Kaynak: https://firebase.google.com/docs/ai-logic/pricing

Önceki ana-proje canlı sentetik kontrolünde App Check doğrulaması geçti; sağlayıcı **prepayment credits depleted** hatası döndürdü. Bunun ardından kullanıcı demo maliyet tercihi olarak yalnız AI erişimini ayrı Spark projesine taşımayı onayladı. Yeni `financeai-ai` projesinde billing bağlı değil; API/App Check/Play Integrity yapılandırması salt-okuma ile doğrulandı. Ücretsiz erişim kota ve sağlayıcı koşullarına tabidir; yeni projenin gerçek AI yanıtı henüz doğrulanmadı. Kredi yükleme/billing değişikliği yapılmadı. Kaynaklar: https://firebase.google.com/docs/ai-logic/pricing ve https://ai.google.dev/gemini-api/docs/billing#prepay

AI isteği önceki kapsamı korur: tüm işlem geçmişi (tarih/tür/kategori/tutar/not), bütçeler ve aynı takvim ayı için uygulamada hesaplanan özet. Owner ID, token, dosya/fotoğraf URI'sı veya koordinat eklenmez. System instruction kullanıcı sorusu/notundan ayrıdır; rapor JSON olarak kodlanır. Önceki sohbet geçmişi modele bağlam olarak eklenmez. Sağlayıcı veri kullanım koşulları billing ve ürününe göre değerlendirilir; Gemini koşulları: https://ai.google.dev/gemini-api/terms

## Git geçmişi

Güncel source Manifest'te literal Maps anahtarı yoktur; yerel Git geçmişinde eski Maps değeri vardır. Firebase istemci config'i takip edilir. Geçmişi yeniden yazmak veya anahtar rotation otomatik yapılmaz. Yerel current/history taramasında private key/keystore/service-account/.env içeriği bulunmadı; canlı GitHub görünürlüğü ayrıca doğrulanmadı.
