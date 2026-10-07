# Faz 4 — AI ve dış servis yapılandırması

**Son dil/KDoc değişikliği:** Ay/yıl artık numeric JSON olarak gönderilir, yanıt dili hızlı XML/manüel soru metninden belirlenir. AiFirebaseApp açıklaması ikinci Spark AI projesi/default Firebase sınırını anlatır. Son JVM121/121, cihaz36/36 (FCM izolasyonu/default restore), debug/release ve lint0error başarılı. Canlı AI yanıtını kullanıcı kontrol edecek; model/LOW/timeout/retry korunur. Ayrıntı `docs/refactor-ai-project-progress.md`.

**Son aylık AI kapsamı:** Kullanıcı onaylı aylık analiz ve doğal sohbet talimatları uygulandı; eski tüm-geçmiş raporu kararı yerine yalnız aktif takvim ayı kullanılır. Toplam/kategori/bütçe hesabı uygulamaya ait; kayıt/sohbet geçmişi saklanır. Son JVM121/121, cihaz35/35 (test FCM izolasyonu ve restore ayrıntısı devam kaydında), derlemeler başarılı, lint0error/133warning/10hint. Model/düşünme/timeout/retry korunur; gerçek yanıt kontrolünü kullanıcı yapar. Güncel devam `docs/refactor-ai-project-progress.md` başındadır.

**AI gecikme düzeltmesi kapanışı:** Yeni model eski SDK/default180s ile bırakıldığı için Firebase AI SDK17.17.0, LOW thinking, bounded timeout/retry ve ayrı access/network/timeout/server mesajları uygulandı. Son JVM117/117, debug/release ve test APK derlemesi başarılı, lint0error/133warning/10hint. Kullanıcı yeni canlı AI kontrolünü uygulamadan kendisi yapacak; agent generateContent göndermedi. Kayıt `docs/refactor-ai-project-progress.md` başındadır. Tasarım, finansal kayıt kapsamı ve hesap/persistence kuralları korundu.

**Token sonrası dış kontrol:** Kullanıcı token kaydını tamamladı; yeni projenin gerçek SDK App Check kontrolü geçti. Model çağrısı zaman aşımı / sağlayıcının yoğun talep hatasıyla sonuçlandı; başarılı AI cevabı açık kalan dış adımdır. Son kayıt `docs/refactor-ai-project-progress.md` içindedir.

**Güncel devam:** Kullanıcı demo AI erişimi için ayrı Spark projesini onayladı. Model `gemini-3.8-flash`, yalnız AI erişimi named `finance-ai` app üzerinden `financeai-ai` projesinde; ana Firebase istemcileri ve finansal/sohbet verisi korunuyor. Güncel kapanış **109 JVM / 32 cihaz testi**, lint 0 hata; ayrıntı ve kalan token kaydı/gerçek yanıt sırası `docs/refactor-ai-project-progress.md` içindedir. Aşağıdaki tek-proje/prepayment kayıtları önceki uygulama ve dış engelin tarihçesidir.

7 Ekim 2026. Kullanıcı “tamam faz 4 uygula” ile uygulamayı başlattı. Altın Kurallar, Test Kalitesi ve 21/23/30/32 maddeleri tamamen okundu. Başlangıç checkout temiz; önceki altı ek refaktör commit edilmiş durumda.

## Kapsam ve korunan kararlar

- Credential Manager explicit Google button akışı; yalnız önceden kayıtlı kullanıcı giriş politikası, Firebase provider/oturum ve iptal davranışı korunur.
- Firebase AI Logic / Gemini Developer API; mevcut `gemini-2.5-flash` ve finansal analiz/geçmiş kapsamı korunur. SDK, yerel sohbet kaydı, snapshot ve request/retry ayrı sahiplerde.
- Hatalar AI cevabı olarak kaydedilmez; aynı request yeniden denendiğinde kullanıcı/cevap satırları çoğalmaz, hesap değişimi geç sonuçları reddeder.
- Maps Manifest placeholder, yerel/CI config, Gemini BuildConfig alanının kaldırılması ve sır dosyaları ignore kontrolü.
- AI/Auth gerekli dependency/catalog ve typed compilerOptions; faz 5 navigation/tema/genel build temizliği ve README kapsamı başlatılmaz.
- UI tasarımı ve normal kullanıcı akışı korunur; hata sunumunda gereken somut karar ayrı takip edilir.

## Dış kararlar

Functions için Blaze'a geçildiğinden eski Spark/free AI varsayımı geçersizdir. Firebase'in güncel belgesine göre billing bağlı projede Gemini Developer API kullanımı ücretli olur. Kullanıcı düşük hacimli canlı kullanımı ayrıca onayladı. Aynı model ve veri kapsamı korunur; bu onay yeni ücretli model, limitsiz çağrı veya yeni Console/billing ayarı değildir. Kaynak: https://firebase.google.com/docs/ai-logic/pricing

Kullanıcı AI hata/retry sunumunu onayladı: yalnız hata durumunda Toast, başarısız metni draft'a geri koyma, aynı request kimliğiyle yeniden gönderme ve hata metnini sohbet cevabı olarak kaydetmeme. Normal sohbet tasarımı korunur.

## Doğrulama ve devam

- Başlangıç mevcut JVM suite çalıştı: `:app:testDebugUnitTest --offline`, BUILD SUCCESSFUL. Bu başlangıçtır, yeni Faz 4 kaynaklarının kapanış kanıtı değildir.
- Gradle için Java21 ve sandbox dışı cache erişim izni alınabildi; eski altı-refaktör kaydındaki ortam engeli bu tur tekrar edilmedi.
- Son adımlar: risk odaklı Google policy/AI request/account/cancellation/financial snapshot testleri; tüm JVM ve cihaz suite, debug/release derleme, lint, Node/Rules testleri; kullanılmayan dependency/import/XML/log ve Git sır türü taraması; kapsam kapanış eşlemesi.

Deploy, billing değişikliği, anahtar rotation, history rewrite ve commit/push yapılmadı. Aşağıdaki iki sınırlı Console düzeltmesi kullanıcıdan ayrıca alınan açık onayla yapıldı.

## Canlı erişim — en güncel kayıt

- Salt-okuma Console kontrolünde Maps package/API kısıtları, Firebase API hedefleri, AI traffic filter ve App Check enforcement okundu. Maps'in mevcut sertifikası Mac debug SHA1 ile farklı; yalnız bu Mac'in tuple'ını ekleme kararı kullanıcıya soruldu.
- App Check 403 nedeniyle mevcut emülatör debug tokeni ignore edilen yerel dosyaya hazırlandı. Kullanıcı **yalnız bu tokenin eklenmesini** onayladı. Android app `1:1048183018405:android:a8024344a1552fb3508e4c` için token sayısı 1→2 oldu, mevcut kayıt korundu; enforcement değişmedi.
- Geçici canlı probe ilk turda izole `Application` test harness'i nedeniyle FCM/Hilt hizmeti başlatınca durdu. Gerçek `FinanceApplication` ile ayrı turda App Check geçti, model/provider aşamasına ulaşıldı.
- Sağlayıcı açık hata döndürdü: **ön ödeme kredisi tükenmiş/yok**; AI Studio'da billing/prepayment istiyor. Bu gerçek AI cevap kanıtı değildir. Model değiştirilmedi, billing ayarı değiştirilmedi, ödeme yapılmadı, yeni canlı çağrı durduruldu. Kaynak: https://ai.google.dev/gemini-api/docs/billing#prepay
- Geçici ücretli probe kaldırıldı ve normal `FinanceTestRunner` geri getirildi. Otomatik suite gerçek AI çağrısı yapmaz. Bu kaynakların kaldırılması gerçek erişim engelini kapatılmış göstermez.

## Kapsam kapanış eşlemesi

- **21:** Legacy GoogleSignIn SDK ve module kaldırıldı; explicit Credential Manager provider seçimi Route'ta, SDK'sız `GoogleIdentity` ve kayıtlı-hesap kontrolü `SignInWithGoogle` use case'inde. Firebase geçişi seri repository işlemi olarak kaldı; iptal/collision/verification/logout anlamı korundu. Provider credential session logout'ta bounded temizlenir. Kotlin typed `compilerOptions` ile JVM11 korunur. Phase2/3 tarih/konum altyapısı tekrar kurulmadı; CameraX/Nav3/major upgrade eklenmedi.
- **23:** Maps literal, environment/Gradle property/ignore edilen local configuration ve Manifest placeholder'a taşındı. Build'de eksik Maps config açık hata verir; değer çıktıya dökülmez. Gemini BuildConfig/local key alanı kaldırıldı, BuildConfig özelliği korundu. Firebase istemci JSON aynı app/project'e bağlı ve Google debug sertifikası/web client kaynakları doğrulandı. Private-key/keystore/.env/credentials ignore ve current/history taraması tamamlandı. Git geçmişindeki eski Maps ve Firebase istemci config'i ayrı listelendi; GitHub görünürlüğü veya history rewrite/rotation yapıldığı iddia edilmedi. Kullanıcının ek onayıyla Maps debug SHA tuple'ı eklendi; eski kayıt/API kısıtları korundu ve API ile yeniden doğrulandı.
- **30:** BoM tarafından çözümlenen Firebase AI Logic 17.2.0 ve Gemini Developer API backend'i kullanıldı, mevcut `gemini-2.5-flash` korundu. SDK data/generation adapter'ında, sözleşmesi SDK'sız domain'de. Room conversation store, atomik financial snapshot, saf ortak finance/budget hesabı, JSON prompt ve system instruction sahipleri ayrıldı. Yerel mesaj+outbox atomic; Firestore sync model isteğini bekletmez. Stable request/reply kimlikleri, serial send guard, explicit same-request retry, cancellation ve owner/generation reddi eklendi. Ham SDK hata metni sohbet cevabı olarak kaydedilmez. Kullanıcının onayıyla yalnız hata anında Toast/draft restoration uygulanır. Tüm işlem geçmişi, mevcut not/tarih/kategori/tutar ve bütçeler korunur; budget-only hesap rapordan düşmez. Tarihlerin aynı takvim zone'unda yorumlanabilmesi için zone JSON'a açık yazılır; para birimi hesap currencyCode'dan alınır. Yeni sohbet hafızası/otomatik retry/retention/global backend kurulmadı.
- **32 (Faz4 kısmı):** AI/Auth dependency ve BoM/catalog bağlantıları, kotlinOptions modernizasyonu ve doğrudan kullanılan SDK sınırları doğrulandı. Eski Google AI client ve direct legacy Play Auth bağımlılığı kaldırıldı. Genel Room/Activity/Material/diğer catalog temizliği ve wrapper executable konusu Faz5'e ait olmaya devam eder. README değiştirilmedi.
- **Kalite/temizlik:** Category label resource adapter'ı presentation-only paketten feature `transaction/format` sahibine taşındı; UI ve AI tek eşlemeden faydalanır. Eski Google module, eski AI report XML'leri (12) ve gereksiz current-month helper temizlendi. Testler üretim paketini aynalar. Temporary probe/runner değişikliği ve boş probe klasörü kaldırıldı; güvenli hata logları kaldı, finance/token/key dump eklenmedi. Room v15/schema hash değişmedi; migration/version bump yok.

## Son doğrulama

- Tam Java21 Gradle rerun: `testDebugUnitTest assembleDebug assembleDebugAndroidTest compileReleaseKotlin lintDebug --rerun-tasks --offline`, **112 task executed, BUILD SUCCESSFUL**. Son timezone metadata değişikliği sonrası aynı beş task ayrıca güncel kaynakla **BUILD SUCCESSFUL** (48 saniye, 46 executed/66 up-to-date); JVM test task gerçekten yeniden çalıştı ve XML sonuçları tekrar 105/105 olarak sayıldı.
- Mevcut suite **105/105 JVM**, failure/error/skipped 0. Retry/de-duplication, same-account generation/account switch, cancellation, overlapping send, failed draft/new draft, Google registered/unknown/collision policy, financial month/decimal/budget-only ve expected network/invariant testleri dahil.
- Son normal runner/device suite **31/31**, tüm mevcut testler; gerçek Room request identity/timestamp/outbox ve structured JSON/question/note/zone ayrımı da dahil. Son APK kuruldu ve tam instrumentation turu tekrar geçti. Cihaz kurulumunu korumak için APK `adb install -r` ve doğrudan instrumentation kullanıldı; debug token tekrar üretilmedi.
- Functions **59/59**, Rules **16/16** (yalnız `demo-financeai` Firestore/Storage emulator) geçti. Bu Faz4'te sunucu kodu/Rules/deploy değişmedi.
- Lint **0 error, 132 warning + 10 hint**, **UnusedResources 0**. Paket/path aynalama taraması ve `git diff --check` temiz. Bunlar sıfır global uyarı veya bütün uygulama manuel kabulü anlamına gelmez; kalan genel UI/build tutarlılığı Faz5'tedir.
- Google provider sistem seçicisi ile gerçek kullanıcı oturum açma ayrıca cihaz etkileşimi ister; otomatik policy testleri ve doğru cert/client config bu manuel girişin yapıldığı anlamına gelmez.
- **Açık dış adım:** sağlayıcı billing/prepayment kredisi olmadan AI cevabı veremiyor. Android/App Check erişimi doğrulandı, gerçek yanıt başarısı doğrulanmadı. Kullanıcı AI Studio ödeme tercihine karar verir; ön ödeme veya plan değişikliği kendiliğinden yapılmaz.

## Devam noktası

Faz4'ün onaylı yerel kod, dependency/config, temizlik ve otomatik doğrulama kapsamı tamamlandı. Açık kalan gerçek AI yanıt kontrolü **Gemini prepayment** dış engeline bağlıdır; Faz4'ün bütün dış erişim hedefi tamamlandı diye raporlanmaz. Kullanıcı kredi/ödeme kurulumunu tamamladığını söylerse yalnız tek sentetik AI yanıt kontrolü yeniden yapılır; otomatik suite'e ücretli test eklenmez. Sıfır kredi ile model değişikliği aynı billing engelini kendiliğinden çözmez. Faz5, type-safe navigation ve README kullanıcı ayrıca başlatınca uygulanacak.

App Check ve Maps izinli düzeltmeleri API ile yeniden kontrol edildi. Başka Console/IAM/Rules/billing/history mutasyonu veya commit/push yapılmadı. Kaynakları taşıma/yeni test yazma işi baştan tekrarlanmaz; bu devam kaydı ve güncel Git durumundan sürdürülür.

## Kullanıcı onaylı credential sadeleştirmesi

GuideMate'in güncel kullanımı incelendi ve kullanıcı aynı oluşturma düzenini istedi. Ayrı `CredentialModule` ve SDK manager inject eden `CredentialSession` kaldırıldı. `auth/data/local/session/CredentialSessionManager`, application context'i inject edip `CredentialManager.create(context)` çağrısını kendi içinde yapar; dar `CredentialSessionCleaner` sözleşmesini uygular. Var olan `AuthModule` yalnız `@Binds` bağlantısını içerir; yeni ayrı SDK provider module yok. Singleton scope yalnız manager sınıfında bulunur, binding'de tekrar edilmez.

`AuthRepositoryImpl` cleaner sözleşmesini tüketir. Aynı `clearCredentialState(ClearCredentialStateRequest())` çağrısı, yerel sign-out sırası, 2 saniyelik sınır, cancellation ve güvenli hata logu korunur. GuideMate'teki adapter içi hata yutma burada kopyalanmadı; FinanceAI'nin mevcut repository hata politikası aynen korundu. Giriş ekranındaki provider seçicisi, SDK/dependency/Manifest/Console ve tasarım değişmedi. Yeni düşük değerli test eklenmedi; mevcut JVM suite ve debug/release derleme kontrolü yeniden çalıştırıldı: **BUILD SUCCESSFUL, 105/105 JVM**, debug APK ve release Kotlin başarılı, Hilt graph derlendi. `git diff --check` temiz. Bu küçük DI düzenlemesi sonrası cihaz veya canlı sağlayıcı turu yeniden çalıştırılmadı; önceki 31 cihaz/59 Node/16 Rules sonucu önceki Faz4 kapanışına aittir.
