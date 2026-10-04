# Faz 1 — Canlı uygulama ve devam kaydı

Güncelleme: 5 Ekim 2026. Kullanıcı Faz 1'e başlama talimatı verdi. **Faz 1 devam ediyor; tamamlandı sayılmadı.** Bu dosya güncel uygulama kaydıdır; audit-progress içindeki eski “üretim başlamadı” notları tarihsel kararlardır.

Devam ederken önce değişim listesindeki Altın Kurallar ve Test Kalitesi, ardından bu kayıt ve Faz 1'in 1–7/10 kararları okunacak. Güncel Git durumunu doğrula; bitmiş taşıma ve testleri baştan yapma. Her fazda gerçekten kullanılmayan kod/kaynak/boş paket ve geçici konsol logları temizlenir; bilinçli uyumluluk parçaları korunur.

## Uygulanan mimari ve paket temeli

- Tek Gradle app modülü; app/core/feature düzeni. Auth, transaction, schedule, budget, home, aichat ve location ekranları kendi presentation sahibinde. Auth signin/signup/passwordreset; transaction add/history/detail alt paketlerinde gruplanır. Her ekran için boş data/domain açılmadı.
- Repository sözleşmeleri feature domain/repository, implementasyonları feature data/repository altında. FinanceRepository/Impl kaldırılıp TransactionRepository/Impl ve ScheduledTransactionRepository/Impl ayrıldı. Feature Hilt binding'leri Binds ile kendi di paketlerinde; ortak Firebase/Room/Work/dispatcher sağlayıcıları core altında. Singleton kapsamları korunur.
- Transaction, ScheduledTransaction, Budget, AiMessage domain modelleri ve data mapper'ları ayrıldı. Presentation artık bu modelleri kullanır, Room entity kullanmaz. DAO/entity feature data/local; FinanceDatabase/Converters core/database altında.
- FinancialTypes transaction domain, BudgetType budget domain altında. Tablo, alan, enum adı/değeri, DB version 13, ID, tutar Double ve tarih temsili değişmedi. Dört taşınan entity tanımı Git'teki başlangıç ile package/import/whitespace hariç karşılaştırıldı; hepsi aynı.
- Flow API adları gerçek anlamına göre observe… oldu; tek seferlik suspend get… çağrıları korunur. TransactionDetailScreen/ViewModel, AiChatViewModel ve ScheduledTransactionsViewModel adları açıklayıcı hale getirildi. DatePickerField dosya yazımı düzeltildi. Kullanıcının Edit* adlarını koruma istisnası uygulandı.
- Auth repository SDK'sızdır: Firebase AuthResult/GoogleSignInAccount domain API'sinden çıktı. User Firestore DTO data/remote; AuthException domain/error altında. SignInWithPassword giriş + e-posta doğrulama yenileme sırasını sahiplenir; Firebase reload data sınırına alındı. Signup/Firestore kaydı ve e-posta gönderme private data işlemleridir.
- Ortak AuthViewModel kaldırıldı. SignInViewModel, SignUpViewModel, PasswordResetRequestViewModel, PasswordResetViewModel ve SessionViewModel gerçek ekran/oturum sorumluluklarına ayrıldı. AuthFormValidation mevcut kuralları ortaklaştırır. Form, state, hata ve Google giriş sonuçları korunur; yeni ürün kuralı eklenmedi.
- TransactionSync, BudgetSync ve ScheduledTransactionSync dar SDK'sız feature sözleşmeleri eklendi. Finansal VM'ler artık doğrudan FirebaseSyncService almaz. App SyncBindingsModule bu sözleşmeleri geçici olarak mevcut aynı Singleton servise bağlar; yeni scope/Dependencies bag yok. Sync iç sırası/başarı semantiği bu adımda değişmedi.
- FinanceApplication app altında, Manifest application referansı güncellendi. Navigation/graph/deep-link/bottom-nav app/navigation altında; dış route değerleri değişmedi. MainActivity yaşam döngüsü app/MainActivity altında; root MainActivity mevcut launcher/PendingIntent kimliğini koruyan AndroidEntryPoint shim olarak tutulur.
- Tema core/ui/theme, ortak Edit* UI/Dropdown core/ui/component; route sahibi EditTopBar app/presentation/component. Splash app/presentation/splash, formatlama core/format, grafik home/presentation/component, kategori kaynak/icon mapper'ları transaction/presentation/mapper altında.

## Her fazda temizlik — şu ana kadar yapılanlar

- 33 boş eski kaynak klasörü içeriksiz olduğu tek tek doğrulanarak rmdir ile kaldırıldı. Eski taşıma yollarındaki dosyalar silindi; yaşayan kod yeni sahiplerinde. Eski monolitik repository/AuthViewModel kalmadı.
- Kullanılmayan transaction Firestore-ID delete API/DAO, category expense sorgusu; schedule pending sorgusu/Firestore-ID delete API/DAO; AiMessageDao.clearAllMessages; BudgetDao.deleteBudgetByFirestoreId kaldırıldı.
- Domain sendEmailVerification gereksiz public API olmaktan çıktı; yalnız data private kullanımı kaldı. AI domain→entity mapper üretimde kullanılmadığı için kaldırıldı; entity→domain history eşlemesi korunur.
- Kullanılmayan/same-package/tekrar importlar temizlendi. Kotlin getValue/setValue operatör importları gerçek delegate kullanımı için korunur.
- Lint ve kaynak/XML referans kontrolüyle doğrulanan 31 kullanılmayan string, 7 kullanılmayan template renk ve 2 kullanılmayan drawable launcher şablonu kaldırıldı. Kullanılan mipmap launcher ikonları ve aynı isimli aktif color kaynağı korunur. Manifest/dinamik getIdentifier/RemoteViews referansları da kontrol edildi. Son lint raporunda UnusedResources: 0.
- Geçici println/Log.d/Log.v yoktu. printStackTrace ve hassas ham exception mesaj/payload logları bağlam + exception sınıfı içeren gerekli teşhis loglarına dönüştürüldü. Hata sonuçları/state/work sırası değişmedi; bütün loglar körlemesine silinmedi.
- Kaldırılan takipli eski kaynaklar Git'ten geri alınabilir; boş klasörler zaten içeriksizdi. Kullanıcının mevcut dosyaları silinmedi.

## Testler

Yeni 28 anlamlı JVM senaryosu ve mevcut 1 örnek test: toplam 29.

- SignInWithPassword: 5; giriş/doğrulama sırası, credentials, doğrulanmamış hesap, giriş/yenileme hatası, cancellation propagation.
- Mapper'lar: 6; transaction 2, schedule 2, budget 1, AI history 1. Kimlik/alan/null/media/flag/timestamp/sıra korunması.
- Auth presentation/validation: 13; signup 4, reset request 3, reset 2, session 1, validation 3. Başarı/hata ve mevcut sınırların korunması.
- TransactionRepositoryImpl: 4; insert ID/alanlar, liste sırası ve eşdeğer emission, null/mevcut detay, read failure'ın boş başarıya dönüşmemesi.
- Session testi yalnız repository çağrısının dönüşünden önce callback verilmemesini doğrular. Repository içindeki eski fire-and-forget logout'u dayanıklı hale getirdiği iddia edilmez.
- Test paketleri üretim paketini aynalar. MainDispatcherRule core/coroutines/testing, gerçekten paylaşılan FakeAuthRepository feature/auth/testing altında; tek-suite DAO fake kendi test dosyasında.
- Coroutine-test JVM bağımlılığı 1.9.0 eklendi; dependencyInsight mevcut çözülen runtime core sürümünün 1.9.0 olduğunu doğruladı. Runtime coroutine sürümü yükseltilmedi. Framework/coverage için yeni test yok.

## Gerçek doğrulama

Son kontrol, kaynak temizliği SONRASI:

JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home bash gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --rerun-tasks --offline --console=plain

- BUILD SUCCESSFUL; 38 saniye, 62 task'ın tamamı executed.
- JUnit XML: 29 test; 0 failure, 0 error, 0 skipped. Bunların 28'i yeni risk testidir; 1'i mevcut örnek test.
- Debug APK derlendi. Cihaza kurulum/giriş veya görsel davranış doğrulaması yapılmadı.
- Lint: 0 error, 137 Warning ve 10 Hint. UnusedResources 0; uyarısız/sıfır kusur denmez. Kalanlar ağırlıkla dependency/catalog, icon, typo/plural/typography ve modifier önerileri; ilgili sonraki fazlarda gerçek ihtiyaç/görünüm sınırıyla değerlendirilecek.
- git diff --check temiz. Statik tarama: boş kaynak klasörü 0; duplicate import dosyası 0; feature domain'de Android/Firebase/Google veya data/presentation import ihlali 0; println/printStackTrace/Log.d/Log.v yok.

Önceki başarısız denemeler gizlenmez:

- İlk sandbox Gradle zip-lock izni nedeniyle çalışmadı; gerekli Gradle cache yetkisiyle çalıştı.
- Hilt eski generated application referansı taşıma sonrası hata verdi; app:clean yalnız generated build çıktısını temizledi, yeniden derleme geçti. Kalıcı uygulama/Room verisi silinmedi.
- Taşıma/import/FCM injection compile hataları yakalanıp düzeltildi; sonraki gerçek derlemeler geçti.
- İlk offline lint koşusu cache'te lint-gradle 31.13.0 olmadığı için başarısızdı; online normal dependency çözümüyle edinildi, lint ve son offline koşu geçti.
- Repository hata testi coroutine stacktrace recovery nedeniyle nesne kimliği assertion'ında başarısızdı. Aynı exception instance'ına bağlanmak yerine tür/mesaj doğrulandı; son rerun geçti. Üretim hatası yutularak test geçirilmedi.

## Bilinçli kalan sınırlar — kullanılmayan kod değildir

- FirebaseSyncService hâlâ büyük/çok sorumluluklu ve 9 constructor bağımlılığı var. Dar tüketici sözleşmeleri hazır; feature-owned mapper/sync, hesap scope'u, durable outbox ve gerçek başarı/iptal ayrımı Faz 2. Parametreleri tek dev bag'e gizleme yok.
- Auth data logout hâlâ eski clearAllTables/fire-and-forget; SessionViewModel finally callback politikası korunur. Faz 2 veri koruma/hesap geçişinde somut davranış farkı açıklanıp gereken karar alınacak. Genel cancellation güvenliği henüz tamamlanmadı.
- VM'lerin WorkManager/photo/Context/LocationUtil altyapı bağlantıları ve persisted receiver/service/worker kimlikleri Faz 3 ile birlikte düzenlenecek. Notification/FCM/photo/firebasesync root paketleri bu yüzden şimdilik var; “unused” diye silinmez.
- SignInViewModel/Screen eski Google SDK adapter'ını kullanır; Auth domain SDK'sızdır. Credential Manager ve eski Gemini SDK geçişi Faz 4. AI data hâlâ presentation kategori resource mapper'ına bağlıdır; rapor/provider sınırı Faz 4'te ayrılacak. Tüm katman ihlallerinin bittiği söylenmez.
- Splash doğrudan FirebaseAuth kullanır; oturum/startup readiness Faz 2. Alt UI bileşenlerin VM yerine state/callback tüketmesi, typed navigation, state/lifecycle/hata/tema bütünlüğü Faz 5.
- Root MainActivity compatibility shim, Hilt/Room generated girişler, receiver callback ve framework override'ları gerçek kullanımdır; metinsel çağrısı yok diye silinmez.
- DB destructive migration, money/date temsili ve görünen tasarım değiştirilmedi. Açık currency/conflict/legacy ownership/backup/notification ürün kararları otomatik seçilmedi.

## Eksiksiz devam için sıradaki adım

1. Faz 1 1–7/10 kapsamının son constructor/parametre/görünürlük/DI-sahiplik incelemesini, bu kayıtlı sonraki faz sınırlarıyla birlikte bitir. Gerçek faz-1 eksikliği varsa mevcut davranışı koruyarak uygula ve gerekli testini çalıştır; sonradan yapılacak UI/SDK/durable sync işini bitmiş gösterme.
2. Faz 1 kapanışını değişim listesinde uygulandı/doğrulandı/kalan faz olarak açık eşle. Bu kayıt şu anda kapanış değildir; Faz 2'yi otomatik başlatma.
3. Her yeni faz öncesi Altın Kurallar + Test Kalitesi ve ilgili kararları tamamen oku; kullanıcının güncel yetkisine göre somut plan sun. Her fazın kullanılmayan kod/kaynak ve log temizliğini tekrarla.
4. Genel son test kapsam taraması, cihaz/Compose/görsel kontrol ve README kullanıcının ayrıca istediği son aşamada. Bu tur bunlar yapılmış sayılmaz.

## Kullanıcı değişiklikleri ve yetki sınırı

.idea/markdown.xml staged, app/google-services.json modified, app/src/main/res/drawable/ic_notification.xml staged kullanıcı değişiklikleri korunur; overwrite/silme yok. GuideMate, Git commit/push/history, Console, anahtar rotation veya ücretli plan değişikliği yapılmadı. Tasarım/işleyiş asla değişmez; farklı ürün davranışı gerektiren adım ayrı somut onay ister.
