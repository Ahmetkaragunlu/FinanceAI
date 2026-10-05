# Faz 2 — Uygulama, doğrulama ve eksiksiz devam kaydı

Güncelleme: 5 Ekim 2026. **Faz 2 tamamlandı.** Özgün 8 (veri), 15–17 ve 24–26 ile üç ek onay birlikte uygulandı/doğrulandı. Home takvim ayı onayı uygulandı; negatif progress çizimi kullanıcı kararıyla korunur. Faz 3–5, son genel kabul turu ve README başlatılmadı.

## Son talimat ve onaylanan üç ek karar

1. Logout yerel/pending kayıtları silmez. A hesabının veri/dosya/bekleyen işi B hesabına görünmez ve B adına gönderilmez; A yeniden girişte kendi işi devam eder. Aynı telefonda birden fazla hesap desteklenir.
2. Yerel/uzak sürümler korunur. Yalnız gerçek örtüşen değişiklik veya düzenleme/silme çatışmasında kullanıcı seçer; farklı alan değişiklikleri birleştirilir, aynı sonuç için soru sorulmaz. Seçim uygulanırken uzak sürüm tekrar doğrulanır.
3. İlk hesabın para birimi cihazın **bölgesinden** belirlenip hesapta kalıcı currencyCode olarak tutulur; dil tek başına para birimi değildir. Aynı hesap başka cihazda mevcut para birimini kullanır; sonradan locale değişmesi sadece gösterimi etkiler. TRY zorunluluğu/otomatik kur/multicurrency yok.

Eski geliştirme kayıtları kullanıcı için önemsiz; bu ifade silme izni değildir. v13 tabloları legacy_v13_* olarak korunur, sahibi bilinmeyen satırlar gizli kalır ve rastgele giriş yapan hesaba atanmaz. Ana belgedeki offline-first Altın Kural 10 kullanıcı talimatıyla kaldırıldı; mevcut mimari korunur, her faza zorunlu ek offline geliştirme işi çıkarılmaz.

## Başlangıç ve değişmez sınırlar

- Başlangıç temiz Git: HEAD 9b74276 (refactor: reorganize feature-first architecture and responsibility boundaries). Faz 1 kullanıcı tarafından commit edildi; burada commit/push yapılmadı.
- Altın Kurallar, Test Kalitesi, özgün Faz 2 ayrıntıları ve bu devam kaydı okundu. Küçük somut uygulama/test planları üzerinden ilerlenmiştir.
- Normal ekran tasarımı ve akış korunur; kabul edilmiş veri koruma/para doğruluğu düzeltmeleri ve yalnız gerçek çatışmada seçim diyaloğu sınırlı istisnalardır. Görsel eşdeğerlik cihazda manuel doğrulanmış diye iddia edilmez.
- Console, Firebase Rules, canlı GitHub, Git geçmişi, anahtar, GuideMate, README veya ücretli servis değişikliği yapılmadı. Phase-4 AI Logic geçişi bu fazda yapılmış sayılmaz.

## Özgün maddelerin karşılığı ve kaynak sahipliği

### 8 — Veri ve iş kurallarındaki gerçek tekrar

- Feature data/remote mapper ve RemoteRecordStore'ları transaction/schedule/budget/aichat sahibindedir. core/sync yalnız ortak outbox/uzlaştırma/koordinasyon sözleşmesini bilir; concrete feature bağımlılığı Set multibinding ile app/di'de kurulur.
- FirebaseSyncService, eski SyncType, üç domain sync facade'ı ve SyncBindingsModule sorumluluk devri tamamlanınca kaldırıldı; yalnız satır sayısını azaltan kozmetik parçalama yapılmadı.
- Home/Budget/AI aynı BudgetCalculations saf kurallarını kullanır; yüzde genel aylık bütçe tabanını korur. Tek FinancialSummary Room projeksiyonu aynı dönemin gelir/giderini birlikte okur.
- Plan tamamlama RoomScheduledCompletion'da atomiktir. Ekran ve receiver aynı CompleteScheduledTransaction sözleşmesini çağırır; ScheduledCompletionCoordinator ortak work/photo devrini yönetir. PhotoWorkScheduler upload işi oluşturma tekrarını kaldırır.
- UI tekrarları, FCM delivery ve tam medya pipeline'ı ilgili Faz 3–5 işleridir; burada bitmiş sayılmaz.

### 15 — Coroutine, iptal ve tamamlanma

- Finansal mutasyon suspend + Room transaction; gözlem Flow. Başlatmak uzak başarı sayılmaz; durable outbox sonuç doğrulanmadan temizlenmez.
- Geniş catch noktalarında CancellationException yeniden atılır. Listener callbackFlow/awaitClose, receiver goAsync/finally finish, FCM service onDestroy cancellation kullanır.
- AuthRepositoryImpl geçişleri Mutex ile seridir. Native auth mutasyon Task'ının tamamlanması dar NonCancellable helper'da beklenir; caller cancellation geçiş kilidini erken bırakıp auth state yarışı üretmez. Bütün workflow NonCancellable değildir.
- Save/detail callback'leri captured account/generation ve form intent kontrolü kullanır. Çift kayıt guard'ı, fotoğraf save-before-commit ve commit-before-old-file-delete uygulanır.
- Receiver kısa iş için 8 saniye bounded'dır; ApplicationScope process survival garantisi sayılmaz. Dayanıklı bildirim/devir işlerinin tam düzenlemesi Faz 3'tedir.

### 16 — ApplicationScope, SupervisorJob ve hesap ömrü

- core/coroutines/di/CoroutineModule gerçek süreç oturum gözlemcisi için singleton @ApplicationScope sağlar: SupervisorJob + injected IO.
- SessionCoordinator auth gözlemcisinin sahibidir; accountJob hesap değişiminde iptal edilir, yeni oturumda yeni job kurulur. Tüm application scope logout'ta iptal edilmez.
- AccountSession owner/currency/generation taşır; eski Flow/callback sonucu yeni oturumda reddedilir. Hesap Mutex'i tutulurken eski child'a join edilmez (deadlock riski); iptal + nesil kontrolüyle sonuç engellenir.
- ViewModel işi viewModelScope'dadır; kalıcı iş Room + WorkManager'dadır. Servis scope'u kendi servis yaşam süresini izler. Genel CoroutineManager/session Hilt component ağı yok.

### 17 — Merkezi dispatcher ve main-safe iş

- Mevcut @IoDispatcher kullanımı korunur. @DefaultDispatcher yalnız AI raporundaki gerçek CPU hesaplamasında kullanılır; gereksiz Main binding/dependency bag eklenmedi.
- PhotoLocalStore/PhotoRemoteCache ve blocking medya/konum sınırlarında injected IO; Room suspend/Flow ve asenkron Firebase await gereksiz IO wrapper'a zorlanmaz.
- Scope yaşam süresi ile dispatcher thread seçimi farklı sahiplikler olarak kalır.

### 24 — Oturum, readiness ve veri izolasyonu

- SessionCoordinator + AccountSession seri startup/login/logout ve hesap hazırlığı sahibidir. Finansal ekranlar hazır aktif hesap olmadan açılmaz; splash keyfi delay yerine hazırlığı bekler.
- Room sorguları aktif owner'a bağlıdır; hesap değişiminde local kayıtlar/pending/AI/veri ve dosyalar diğer hesaba taşınmaz. SignIn/Main geçişi eski hesap back stack/VM'lerini korumaz.
- clearAllTables/cancelAllWork kaldırıldı. Yalnız owner tag'li işler iptal edilir; gösterilmiş uygulama bildirimleri temizlenir. AccountWorkRestorer aynı hesaba ait geçerli plan ve yerel fotoğraf işlerini yeniden kurar.
- Eski UID/generation worker/FCM/action sonuçları reddedilir. Token kayıt/kaldırma logout'u süresiz bekletmez (bounded); uzak token retry/delivery/payload sözleşmesi Faz 3 sınırıdır.
- Yerel account preferences varsa offline hazırlık uzak sync'i beklemez. İlk kez kullanılan hesabın currency profile'ı server transaction ile doğrulanır; ağ yoksa hazır sayılmaz. Geçersiz/bölgesiz locale için sessiz TRY seçilmez, mevcut giriş hata yolu kullanılır. Yeni bir fallback para birimi ürün kararı icat edilmedi.

### 25 — Room kimliği, durable sync ve migration

- FinanceDatabase 13→14; gerçek veri koruyan AccountMigration, schema export (app/schemas), destructive fallback yok. Legacy arşivler, owner/currency alanları, active_account/account_preferences ve sync_records eklenir.
- Financial ve AI remote kimliklerinde owner + remoteId unique; gerçek insert ID döner, uzak apply yerel PK'yi korur. Yeni genel/kategori bütçe key'i owner+logical category ile deterministiktir; nullable general duplicate kontrolü atomiktir.
- Yerel kayıt/değişiklik/silme + pending tek transaction. Tombstone uzak onay gelmeden kaybolmaz; mutationId/revision/baseline geç ack'in yeni değişikliği silmesini önler.
- AccountSyncWorker ağ constraint/backoff/retry ile owner kapsamında çalışır. Kalıcı record failure pending'i koruyup işaretler; geçici hata retry olur.
- AccountSyncEngine server transaction/reconciliation; cache/pending snapshot uzak doğrulama sayılmaz. Missing/removed kayıt server check ile ele alınır; kör upsert/last-write-wins yok.
- Gerçek çatışma SyncConflictDialog ile local/remote sürüm karşılaştırması/seçimi sunar. Yeni remote revision seçim sırasında yeniden kontrol edilir; yeni conflict sessiz ezilmez.
- Fotoğraf kaldırma niyeti ve photo URL/version outbox'ta korunur. RemoteRecordStore.prepare dosya IO'sunu Room transaction dışına taşır; PhotoRemoteCache owner klasöründe versiyonlu cache, geç hesap kontrolü ve başarısız refresh'te eski dosyanın korunmasını sağlar. Cached dosya yeniden upload edilmez.
- Bu client temelinin canlı Firestore Rules/kısıtlarla doğrulandığı veya iki cihazda server exactly-once finans geçişi sağladığı iddia edilmez. Plan→transaction iki uzak dokümanının server atomic/idempotent contract işi Faz 3 sınırıdır.

### 26 — Para, bütçe, dönem ve tarih

- Room/wire Long minor units; currency scale para birimine bağlıdır (JPY/KWD dahil). BigDecimal sınırda HALF_UP, exact form precision, finite/positive/overflow/Double roundtrip guard ve exact integer wire parsing uygulanır.
- Virgül/nokta form parsing ortak; edit küsuratı atılmaz. Home/Budget/History/Schedule/Detail/Add/AI/notification hesap para birimini kullanır; son sabit ₺ XML tüketicisi de kaldırılır.
- Currency profile local ve Firestore users/{uid} üzerinde kalıcıdır; mevcut uzak değer ilk cihaz önerisini geçersiz kılar. Legacy sahibi/para birimi tahmin edilmez.
- Home/Budget/AI ortak budget limit/yüzde hesaplar. Yüzde toplamı/100 üst sınırı kullanıcı seçmeden eklenmez; geçersiz/negatif/nonfinite değerler reddedilir.
- java.time/Clock/FinancePeriods/FinanceCalendar; UTC date picker→local date, exclusive [start,end), gün/ay geçişi ve onResume refresh. Future date yanlış Bugün olmaz.
- AI finans snapshot'ı account lock + Room transaction; toplamlar exact minor unit, budget bölümü current calendar month, genel geçmiş bölümü all-time kalır. Veri kapsamı sessiz daraltılmaz.
- remainingIncomeRatio/remainingBalance ad/formül anlamı düzeltildi; sıfır gelir/eksi bakiye testlidir. **26.5 dönem onayı uygulandı; 26.7 negatif bar çizimi kullanıcı kararıyla korunur**; formül düzeltmesi görünür yeniden tasarım izni değildir.

## Kullanıcı kararları — Kapanışta netleşenler

1. **26.5 onaylandı ve uygulandı:** “Bu ay” başlığı korunur; Home gelir/gider/bakiye ve kategori grafiği takvim ayını kullanır. 1 Ekim 00.00 dahil, 1 Kasım 00.00 hariç; 31 Ekim'in tamamı kapsanır. Budget ve aylık AI bütçe dönemiyle uyumludur. Ay değişimi/onResume yenilemesi korunur. Son bir ay History filtresinin farklı amacı değiştirilmez.
2. **26.7 bilinçli koruma:** Kullanıcı negatif bakiye progress çizimini değiştirmemeyi istedi. Negatif metin ve mevcut tam dolu çubuk korunur; ratio ad/hesabı/testi yapılmıştır, görsel iyileştirme uygulandı denmez. Bu konu yeniden onay sorulacak veya sonraki fazda kendiliğinden değiştirilecek iş değildir.

## Takvim ayı onayı öncesindeki doğrulama (tarihsel dilim)

- Tam kaynak derleme/test/lint: JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home bash gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --rerun-tasks --offline --console=plain. **BUILD SUCCESSFUL, 51 saniye, 93 görevin tamamı executed.**
- JUnit XML: **63 test, 0 failure/error/skipped**; reconciliation/ack, session, para, dönem, financial summary, bütçe ve wire mapper riskleri dahil. Tamamı yeni Faz-2 testi değildir.
- Lint XML: **0 error, 141 warning + 10 hint**, UnusedResources 0. Debug/test APK derlendi; sıfır uyarı garantisi verilmez.
- connectedDebugAndroidTest --offline, class filtresi AccountMigrationTest/TransactionRepositoryImplTest/BudgetRepositoryImplTest/PhotoRemoteCacheTest: Pixel_8a Android 16'da **14/14**, BUILD SUCCESSFUL (11 saniye). JUnit XML: 14 test, 0 failure/error/skipped; önceki 7/11 koşuları toplamın üzerine eklenmez.
- Gerçek Room senaryoları: veri koruyan migration; owner/remote unique; insert ID; logout A→B→A pending koruma; eski hesap mutation reddi; tombstone; exclusive period; outbox hatasında local rollback; exact/account financial summary; remote apply PK/photo retention; explicit photo remove; atomik/tekrarlı plan completion + photo/version metadata; duplicate/fractional budget.
- Cache testi gerçek dosya/owner/versiyon ve failed refresh'te eski dosyanın korunmasını doğrular; gerçek Firebase indirme kullanmaz. Test application FinanceApplication/Firebase başlatmaz; canlı kullanıcı hesabına yazılmaz.
- Test paketleri üretimi aynalar; eski dört mock-only TransactionRepository JVM testi daha anlamlı gerçek Room testleriyle değiştirildi. Yeni test dependency gerekmedi.
- Tam rerun ilk denemesi yeni fixture'ın zorunlu note alanı eksik olduğu için compile hatası verdi; düzeltmeden sonra yukarıdaki 93-görev rerun ve 14-cihaz koşusu geçti.
- Genel UI/Compose/görsel/manual/Google login/Firebase network/App Check/Console Rules/çok cihazlı kabul yapılmadı; E2E/server exactly-once kanıtı değildir.
- git diff --check temiz; commit/push yapılmadı.

## Temizlik, bilinçli kalanlar ve sonraki kesin adım

- Artık kullanılmayan sync facade/service/type/module/API/import ve sabit para sembolü kaynakları kaldırıldı. Geçici println/Log.d/Log.v eklenmedi; kalan hata logları exception sınıfını kullanır, token/kişisel dosya/finans verisi yazmaz.
- PhotoMoveWorker kaynak çağrısı kalmasa da WorkManager'da persisted eski sınıf kimliği için bilinçli bırakılmıştır; migrasyon/retirement Faz 3'te. Framework/Manifest/Hilt/Room girişleri sırf text-reference yok diye silinmez.
- Reminder'ın mevcut 5 saniyelik eski zamanlama/snooze/expiry politikası değiştirilmedi; tam doğru scheduling Faz 3'te. Medya compression/URI/EXIF/orphan cleanup, FCM durable payload/delivery ve Credential Manager/AI Logic/güvenlik Faz 3–4; hata XML/state/lifecycle/components/theme/navigation Faz 5.
- Kaynak/import/boş klasör/debug log temizliği ve son dönem düzeltmesi tamamlandı; kararlar ve kapanış aşağıda kayıtlı. Eski üç karar/dönem/bar konusunu tekrar sorma, bitmiş uygulamayı tekrarlama. Faz 3 ancak kullanıcı talimatıyla başlatılır.
- Kullanıcı verisini wipe/uninstall/destructive migration yapma; commit/push/Console/key/history/README/sonraki faz otomatik başlatma.

## Ek kullanıcı talimatı — Kısa tür adları ve yalnız tekrarlanan singleton (5 Ekim)

- Tüm üretim/JVM/cihaz Kotlin kaynakları ve Gradle Kotlin dosyaları tarandı. Parametre, annotation ve kod gövdesindeki gereksiz tam paket adları 19 Kotlin dosyasında import + kısa ada dönüştürüldü; importlar düzenlendi. İsim çakışan launcher MainActivity için AppMainActivity, Android graphics Color için AndroidColor alias kullanıldı. Sınıf adları/paketleri, Manifest/worker kimlikleri ve davranış değiştirilmedi.
- Yalnız AuthRepositoryImpl sınıfı ile AuthModule binding'inde tekrar scope bulundu. Sınıftaki annotation kaldırıldı; @Binds @Singleton korundu. Üretilen Hilt kodunda bindAuthRepositoryProvider hâlâ DoubleCheck.provider ile paylaşılır; ortak auth Mutex/oturum sıralaması korunur. Diğer singleton'lar, SDK/DAO scope'ları bu dar kullanıcı onayına dahil olmadığı için kaldırılmadı.
- Kalan uzun paket ifadeleri action/application kimliği string'leri ve dokümantasyon URL'leridir; kod türü/fonksiyon referansı değildir. Framework/Gradle kimliklerini import sanıp kısaltma yapılmadı.
- Son doğrulama: dört build/test/lint task --rerun-tasks --offline, BUILD SUCCESSFUL (55 saniye), 93 görev executed. Import sıralamasının ardından aynı task'lar + dört sınıfa hedefli connectedDebugAndroidTest tekrar çalıştı: BUILD SUCCESSFUL (31 saniye), 21 executed / 73 up-to-date; son üretim ve cihaz test Kotlin kaynakları derlendi, JVM test task yeniden çalıştı.
- Son JUnit sonuçları 63/63 JVM ve 14/14 cihaz; failure/error/skipped 0. Lint 0 error, 141 warning + 10 hint; git diff --check temiz. Yeni iş kuralı olmadığından framework/annotation davranışını tekrar eden yeni test yazılmadı. Tasarım/işleyiş kodu değişmedi; genel görsel/manuel kabul yapılmadı.
- Bu ek iş tamamlandı; o dilimde açık olan 26.5/26.7 kararları aşağıdaki kapanışta netleşti. Commit/push/README/Console işlemi yapılmadı; çalışan Gradle oturumu yok.

## Faz 2 kapanışı — 5 Ekim 2026

**Faz 2'nin kayıtlı uygulama kapsamı tamamlandı ve faz doğrulamaları geçti.** Özgün 8 (veri/iş kuralları), 15–17, 24–26 ile üç ek onay birlikte ele alındı; önceki kalanlar üç yeni kararla değiştirilmedi.

Son kullanıcı kararları:
- **26.5:** Home başlığı “Bu ay” kalır; gelir/gider/bakiye ve kategori grafiği takvim ayını kullanır. Yerel saatle ayın ilk günü 00.00 dahil, sonraki ayın ilk günü 00.00 hariçtir. Ekim'de 1–31 Ekim'in tamamı kapsanır; Eylül/Kasım dahil olmaz. Home/Budget/aylık AI bütçe hesabı aynı ay sınırındadır; History'nin farklı amaçlı filtreleri ve AI genel geçmiş kapsamı değişmez.
- **26.7:** Kullanıcı negatif bakiye çubuğunu değiştirmemeyi seçti. remainingIncomeRatio/remainingBalance adları ve hesapları testlidir; negatif metin/tam dolu mevcut çizim bilinçli korunur. Görsel iyileştirme yapılmış sayılmaz ve sonraki faza otomatik uygulanacak iş olarak aktarılmaz.

Kaynak değişikliği HomeViewModel month/observeMonth ve monthlyCategoryExpenses, HomeScreen forwarding adıdır. FinanceCalendar.observeRollingMonth ve artık kullanılmayan ZonedDateTime importu kaldırıldı. UI renk/layout/başlık/progress kodu değişmedi; yalnız ayrıca onaylanan dönem hesabı düzeltildi. Paket/DI/state sahipliği korunur; gereksiz yeni katman veya singleton eklenmedi.

Son gerçek doğrulama:
- Dört task testDebugUnitTest/assembleDebug/assembleDebugAndroidTest/lintDebug **--rerun-tasks --offline**: BUILD SUCCESSFUL, 49 saniye, **93 görev executed**.
- JUnit XML **65/65 JVM**, failure/error/skipped 0. İki yeni FinanceCalendar senaryosu tam ay sınırı ve resume refresh ile yeni aya geçişi doğrular.
- Pixel_8a Android 16'da dört hedefli instrumentation sınıfı **15/15**, BUILD SUCCESSFUL (12 saniye), failure/error/skipped 0. Yeni gerçek Room testi 30 Eylül/1 Kasım'ı dışlar, 1 Ekim ve 31 Ekim'in son milisaniyesini gelir/gider ve kategori özetinde kapsar.
- Lint **0 error, 141 warning + 10 hint**, UnusedResources 0. git diff --check temiz; debug/test APK başarılı. Genel görsel/manual/Google login/canlı Firebase/Rules/çok cihazlı kabul yapılmadı; sınırsız güvenlik veya server exactly-once garantisi verilmez.

Kapsam kapanış karşılaştırması: 8 ortak data/finans/completion; 15 coroutine/iptal/tamamlanma; 16 gerçek application/session/durable scope; 17 gerekli merkezi dispatcher; 24 seri account/readiness/koruma; 25 migration/ID/atomik outbox/tombstone/reconciliation/conflict; 26 currency/minor/parsing/budget/calendar/ratio uygulanmış ve ilgili risklerle doğrulanmıştır. FCM durable delivery/token retry, reminder politikası, tam medya pipeline ve iki cihazlı server completion sözleşmesi zaten Faz 3 sınırıdır; AI Logic/anahtar Faz 4, UI/navigation/error/theme genel tutarlılık Faz 5'tir. Bunlar Faz 2'nin unutulmuş işi diye gizlenmez; önceki faz planındaki ayrı kapsam olarak korunur.

Çalışan Gradle oturumu yok. Commit/push/Console/Rules/key/history/GuideMate/README işlemi yapılmadı. **Faz 3 otomatik başlatılmaz**; kullanıcı istediğinde önce Altın Kurallar/Test Kalitesi, ilgili madde ayrıntıları, güncel Git ve bu kapanış kaydı okunur.
