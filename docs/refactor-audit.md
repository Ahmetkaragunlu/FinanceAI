# FinanceAI — kapsamlı refactor öncesi inceleme raporu

Tarih: 4 Ekim 2026. Bu rapor mevcut çalışma ağacının statik kaynak incelemesidir; uygulamada yeniden üretilmiş hata, başarılı derleme veya başarılı test raporu değildir.

## 1. Sonuç ve kapsam

Projenin iyi bir temeli var: tek Activity, Compose, Hilt, repository arayüzleri, Room suspend/Flow, lifecycle-aware collection ve birçok yerde WhileSubscribed zaten kullanılıyor. Baştan başka framework ile yazmak gerekmiyor. Ancak “şirket seviyesine yakın” hedef için öncelik paket taşımak değil; veri bütünlüğünü, oturum sınırını ve arka plan işlerinin güvenilirliğini düzeltmek.

MVVM var, fakat katman ayrımı kısmi. Clean Architecture bağımlılık yönleri tam korunmuyor. Offline-first yerel kayıt ve anlık sync ile başlamış; kalıcı işlem kuyruğu, silme kaydı, hesap izolasyonu ve açık çatışma politikası olmadığı için dayanıklı offline-first olarak tamamlanmış sayılmaz.

İncelenenler:

- main altında 103 Kotlin dosyası; wc -l toplamı 11.238 satır.
- Dokuz ViewModel; dört DAO, dört entity, dört repository arayüzü ve implementasyonları.
- Bütün auth/ana ekranlar, harita, alt bileşenler, navigation, DI, application, FCM, worker ve fotoğraf kodu.
- AndroidManifest dahil 13 üretim XML dosyası.
- Root/app Gradle, version catalog, properties, wrapper scriptleri, ProGuard, gitignore, README ve IDE XML yapılandırmaları.
- 44 main PNG kaynağı ve repo görselinin dosya/kullanım envanteri. Görsellerin tamamı için görsel tasarım QA yapılmadı; binary dosyaları kaynak kod gibi satır satır değerlendirmek mümkün değildir.

Bilinçli sınırlar:

- Kullanıcı isteğiyle test kaynakları ve test kapsamı denetlenmedi; test yazılmadı/çalıştırılmadı. Gradle, lint, derleme ve cihaz doğrulaması da bu aşamada çalıştırılmadı.
- build/.gradle üretilmiş dosyaları genel üretim kaynak kapsamına alınmadı; anahtar kontrolü için mevcut üretilmiş BuildConfig yalnız boş/dolu sınıflandırmasıyla kontrol edildi. .git uygulama kaynağı olarak değil, tracked dosya/geçmiş ve remote-tracking kanıtı için okundu.
- local.properties değerleri/secrets rapora çıkarılmadı. Yapılandırmanın bu dosyayı nasıl kullandığı incelendi.
- Firebase Console kuralları, indeksleri, anahtar kısıtları ve deploy edilmiş Functions kodu bu checkout'ta yok. İstemci beklentileri incelendi; sunucu güvenliği veya gerçek sunucu davranışı hakkında kesin hüküm verilmedi.
- Bu turda üretim kodu değiştirilmedi. Yalnız bu rapor ve devam kaydı oluşturuldu. Mevcut google-services.json ve staged kullanıcı değişiklikleri korundu.

Öncelikler: P1 = veri kaybı/yanlış veri/oturum veya temel işlev riski; P2 = mimari, sürdürülebilirlik ve belirgin UX/güvenilirlik iyileştirmesi; P3 = ölçülü temizlik/tercih. Hiçbiri “test ile kanıtlandı” anlamına gelmez.

## 2. Kullanıcının her başlığı için açık sonuç

| Başlık | İnceleme sonucu / uygulanacak yaklaşım |
|---|---|
| SOLID | Sync ve AI sınıfları çok sorumluluklu; veri katmanı UI hatasına bağımlı. Sorumlulukları gerçek sınırlar üzerinden ayır; genel BaseRepository/BaseViewModel kurma. |
| Bağımlılıklar | Room/Firebase tipleri ekranlara ve repository arayüzlerine sızıyor. Domain modelleri ve ilgili repository sözleşmeleri SDK'dan bağımsız olmalı. |
| Test edilebilirlik | Context, static Firebase/LocationUtil/PhotoStorageUtil, saat, bağımsız scope ve somut SDK bağımlılıkları kontrolü zorlaştırıyor. Dar sağlayıcılar ve DI önerildi; test denetimi yapılmadı. |
| Genişletilebilirlik | Yeni sync türü merkezi when bloklarının birçok yerine ekleniyor. Tür başına küçük mapper/data-source ayrımı yeterli; plugin framework gereksiz. |
| İsimlendirme | entitiy, budgetrepositroy, DataPickerField, isRemenderEnabled yazım hataları; Screens.AnalysisScreen destination'ı aslında budget; Edit* isimleri anlamı belirsiz. |
| Sayfa/fonksiyon/paket adları | Auth ve history alt ekranlarına sahiplik bazlı alt paket; observe/get ayrımı, tutarlı Route/Screen/UiState ve worker isimleri. Aşağıda somut eşleme var. |
| Hata mesajları | Ham exception mesajları UI'a/veritabanına çıkıyor, bazı hatalar yutuluyor; ortak kategori seçimi/tutar kaynakları yineleniyor. Typed failure + UI mapper önerildi. |
| Kod tekrarı / ortak yapı | Bütçe kuralları, planlanan işlem tamamlama, fotoğraf worker oluşturma, auth kabuğu/şifre alanı, renkler ortaklaştırılmalı. Görünüşü benzer her kart birleştirilmemeli. |
| Uzun ekran / birden fazla iş | AddTransaction ve Detail route/launcher/form/dialog/preview içeriyor. FilledBudget zaten fonksiyonlara bölünmüş; sadece satır sayısı nedeniyle yeniden tasarlanmaz. |
| Aynı pakette birden fazla ekran | auth/signin, signup, passwordreset; transaction/add, history, detail anlamlı. Tek küçük bileşen için zorunlu yeni paket yok. |
| Fazla parametre / constructor | Sync 8, AI repository 7 bağımlılık: çok sorumluluk işareti. EditTextField 13 parametre Material API yüzeyi olduğundan tek başına hata değil. |
| Kod kalitesi / şirket standardı | Veri güvenliği + net sahiplik + immutable state + migration + hata/işletim görünürlüğü gerekiyor; yeni kütüphane sayısı kalite ölçüsü değil. |
| Güncel teknolojiler | İstenen type-safe Navigation Compose geçişi, Credential Manager ve Firebase AI Logic; version catalog/BOM hizası. Zorunlu Navigation 3/Coil 3/CameraX/multi-module geçişi önerilmiyor. |
| by / .value tutarlılığı | Aynı state'i okumada iki biçim de geçerli. Asıl sorun çoklu/public mutable state kaynakları; stil yalnız eşdeğer bağlamlarda birleştirilecek. |
| Hardcoded renk ve dp / merkezi yönetim | Renk ve ölçü sabitleri var. Theme/ColorScheme + az sayıda semantic ek renk/gradient; ortak spacing/shape/layout token'ları. Tek seferlik ölçülere zorunlu global constant yok. Ayrıntı bölüm 19. |
| APK anahtarı / GitHub güvenliği | Maps ve Firebase config tracked/geçmişte; Gemini şu an boş; debug keystore repo dışında, release signing tanımlı değil. Console kısıtları ve canlı GitHub doğrulaması ayrı sınır. Ayrıntı bölüm 18. |
| Metinlerin XML'den gelmesi | Çoğu UI stringResource kullanıyor; HomeViewModel metinleri kod içinde. UI metinleri kaynaklara; teknik loglar ve iş anahtarları strings.xml'e taşınmaz. |
| SupervisorJob / merkezi coroutine scope | Dört bağımsız SupervisorJob scope bulundu. ViewModel scope korunur; session scope iptal/UID sahibi olur; receiver/service kritik işi WorkManager'a devreder. Tek global CoroutineManager önerilmez. Bölüm 20. |
| Coroutine ve scope | viewModelScope doğru temel; receiver/servis/singleton scope yaşam süresi ve cancellation eksik. Kritik kalıcı işler WorkManager + kalıcı kayıt üzerinden. |
| stateIn / WhileSubscribed | Zaten birçok yerde var. UI'a ait cold Flow için uygun; MutableStateFlow'u tekrar stateIn yapmak veya oturum sync'ini ekrana bağlamak doğru değil. |
| collectAsStateWithLifecycle | UI Flow toplama noktaları zaten bunu kullanıyor; düz collectAsState çağrısı bulunmadı. Toptan dönüşüm gerekmiyor. |
| viewModel.login / viewModel::login | onClick = viewModel::login ile onClick = { viewModel.login() } aynı sıfır argümanlı callback için geçerli. Tek çağrıda referans tercih edilebilir; ek iş/argüman varsa lambda gerekir. Performans garantisi değildir. |
| Dispatcher | IoDispatcher mevcut ve bazı repo'larda kullanılıyor; dosya/bitmap ve eski Geocoder main-safe değil. Room suspend/Flow'u otomatik IO ile sarmalama. |
| Mimari ve paket konumu | Mevcut teknoloji-first kök paketler + screens ayrımı kısmi. Tek module içinde feature ownership + data/domain/presentation hedefi yeterli. |
| Ek güvenilirlik başlıkları | Oturum/veri koruma, offline delete, idempotence, para/tarih, fotoğraf/disk, notification izinleri, deep link, backup, erişilebilirlik, süreç ölümü ayrıca tarandı. |
| Over-engineering | Her fonksiyona use case/interface/config class ekleme; her uzun dosyayı küçük dosyalara körlemesine parçalama; topyekûn framework yenileme yok. |
| Test talebi | Bu aşamada yapılmadı. Bütün refactor tamamlandıktan sonra ayrı aşama; güvenli geçiş planı ve schema koruma şimdiden şart. |

## 3. Önce düzeltilmesi gereken veri ve oturum sorunları

### R01 — P1: remote güncellemelerde yerel primary key kayboluyor

Kanıt: [FirebaseSyncService.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebasesync/FirebaseSyncService.kt:220), mapper'lar 220–289; güncelleme çağrıları 554–576. Transaction/scheduled/budget mapper'ı mevcut entity aldığı halde id'yi yeni entity'ye taşımıyor. Default id=0 ile Room @Update mevcut satırı güncellemez. Çok cihazlı düzenlemenin yerelde görünmemesi için doğrudan kod yolu var.

Öneri: mevcut id'yi koruyan açık mapper/copy; local/remote ID'leri ayrı tanımla. Hatalı remote veri için sessiz 0 para ve “şimdi” tarih üretme; kontrollü hata raporu/quarantine. Aynı sorun AI mapper'ında uygulanıyor mu tür bazlı sözleşmede ayrı korunmalı.

### R02 — P1: bazı insert sonrası sync durumları yanlış satıra yazılıyor

Kanıt: [BudgetViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/BudgetViewModel.kt:182) ve [NotificationActionReceiver.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/notification/NotificationActionReceiver.kt:90). insert'in döndürdüğü local ID yok sayılıyor; id=0 entity.copy ile sync flag update ediliyor.

Öneri: repository kayıt sonucunu gerçek kimlikle döndürsün. Sync başarı kaydı aynı version/işlem kimliğiyle yapılsın. Yeni kayıtları yalnız ilk girişte “unsynced” taraması toparlayacak varsayımına bırakma.

### R03 — P1: başarısız sync başarılı işaretlenebiliyor

Kanıt: [DetailViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/history/DetailViewModel.kt:174). syncTransactionToFirebase Result döndürüyor, exception atmaması başarılı olduğu anlamına gelmiyor; hemen syncedToFirebase=true yazılıyor. Add/Schedule/PhotoMove tarafında da Result bazı yerlerde okunmadan geçiliyor.

Öneri: sonuç tek sorumlu katmanda ele alınsın. Ancak sadece onSuccess eklemek de yeterli değil: eski sürüm upload'ı yeni yerel düzenlemeyi “synced” yapmamalı; version/opId karşılaştırılmalı.

### R04 — P1: çıkış veri temizliği yeni oturumla yarışıyor; bekleyen veri kaybolabilir

Kanıt: [AuthRepositoryImpl.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebaserepo/AuthRepositoryImpl.kt:145). Token silme bekleniyor, sync reset, cancelAllWork, auth.signOut; ardından bağımsız scope'ta clearAllTables başlatılıp signOut dönüyor.

Sonuç: yeni login/ilk sync ile eski clearAllTables yarışabilir. Senkronlanmamış işlemler/AI mesajları silinebilir. ClearAllTables fotoğraf dosyalarını temizlemez. Ağdaki token silme isteği çıkışı geciktirebilir; hata yutulduğu için sonuç bilinmez. cancelAllWork hesapla ilgisiz gelecekteki işleri de iptal eder.

Öneri: tek SessionCoordinator/oturum akışı; hesabı UI'dan anında ayır, eski hesabın işleri/listener'ları durdur, yerel hesap sınırını garanti et, temizliğin tamamlanmasını yönet. Bekleyen offline veriyi başka hesaba asla taşıma; account-scoped saklama veya kullanıcıya açık kayıp onayı politikası. Token revoke ağdan bağımsız, tekrar denenebilir olmalı. “Çıkışta her şeyi sil” veya “hiçbir şeyi silme” otomatik çözüm değil.

### R05 — P1: yerel finansal veriler hesap sahibiyle ayrılmıyor

Kanıt: dört Room entity'sinde userId/accountId yok; local repository sorguları tüm tabloya bakıyor. [TransactionEntity.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/entitiy/TransactionEntity.kt:1).

Öneri: account-scoped kayıt/sorgu veya ayrı account database. Aynı scope WorkManager input/tag ve media yoluna da taşınmalı. Migration eski satır sahibini tahmin ederek yanlış hesaba bağlamamalı; mevcut hesabı ve offline veriyi kontrollü geçirmek gerekir.

### R06 — P1: sync reset eski coroutine'leri durdurmuyor

Kanıt: [FirebaseSyncService.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebasesync/FirebaseSyncService.kt:56). resetSync listener/set temizliyor, scope içindeki pull/upload/download işlemlerini iptal etmiyor. auth.currentUser her işlem anında yeniden okunuyor; mutable map/set/flag eşzamanlı erişiliyor. initialize kontrolü ile işin başlaması atomik değil.

Öneri: UID'ye bağlı session job/generation; reset cancel+join/uygun seri geçiş; mutasyonları tek sahip/Mutex ile yönet. Uzak callback geç gelse bile UID/generation kontrolüyle reddet. Gerekli yerlerde inject scope/dispatcher; SupervisorJob tek başına hesap izolasyonu sağlamaz.

### R07 — P1: uygulama yeniden açılınca oturum ve sync hazırlığı eksik

Kanıt: [SplashScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/splash/SplashScreen.kt:27) sadece 3 saniye bekleyip currentUser != null kontrol ediyor. Application/MainActivity restored oturum için sync başlatmıyor; initializeSyncAfterLogin açık login/kayıt işlemlerinde.

Öneri: Authentication state kaynağı; Initializing/SignedOut/VerificationRequired/Ready gibi gerçek oturum durumları. Sabit delay yerine hazırlık sonucu. Splash API isteğe bağlı startup UX iyileştirmesi; iş mantığı Splash composable'da olmaz.

### R08 — P1: e-posta doğrulaması akışın tamamında korunmuyor

Kanıt: [AuthRepositoryImpl.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebaserepo/AuthRepositoryImpl.kt:38) sync ve token kaydını verification kontrolünden önce başlatıyor; kayıt da hemen sync yapıyor. AuthViewModel email signIn'da doğrulama bakıyor, Splash bakmıyor.

Öneri: sync/session ready doğrulamadan sonra; verification resend/reload/yarım kayıt durumu net. Firebase Rules ile server yetkisi de korunmalı; istemci kontrolü güvenlik sınırı değildir. Google provider ve e-posta login farklı koşullarıyla ele alınmalı.

### R09 — P1/P2: Google giriş ve kayıt toparlama sınırları

Kanıt: [AuthViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/auth/AuthViewModel.kt:122) Google credential ile Firebase girişinden önce email üzerinden isUserRegistered sorguluyor; repo Source.SERVER kullanıyor. Uygun kurallarda bu auth öncesi sorgu reddedilebilir; mevcut kurallar görülmediğinden bunu yaşanan Google hatasının kesin nedeni diye sunamayız.

Kayıtta Auth oluştur → email gönder → profil yaz sıralaması var ([AuthRepositoryImpl.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebaserepo/AuthRepositoryImpl.kt:65)). Orta adım hatası eksik profil/yarım kayıt bırakır; tekrar kayıt collision olur.

Öneri: credential sonrası UID tabanlı profil kontrolü, provider-link/collision hatalarının açık eşlemesi; “Google ile yalnız mevcut kullanıcı girebilir” politikasını değiştirmeden çözüm. Yarım kaydı devam ettir/onar; kör Auth silme yapma.

### R10 — P1: gerçek kalıcı offline sync ve silme günlüğü eksik

Kanıt: sync ilk girişte pull/push, sonra listener. İstemci genel SyncWorker/outbox/delete tombstone içermiyor. Silme yerel satırı kaldırıp viewModelScope içindeki child launch'a bırakılıyor ([DetailViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/history/DetailViewModel.kt:195)); ekran kapanışı/süreç ölümü/ağ kesintisinde remote kayıt kalabilir, sonra yeniden indirilebilir.

Öneri: Room'da kalıcı pending operation; yerel değişiklik + outbox kaydı tek transaction. İşler kullanıcı/record/version/opId taşır. WorkManager ağ constraint ile drain eder; retry/backoff/permanent failure ayrılır. Delete işini başarılı remote sonuç gelene kadar takip et; local fiziksel silme zamanını açık politika belirlesin. Firestore cache'i bu yerel silme niyetini kendiliğinden korumaz.

### R11 — P1: conflict ve initial reconciliation uygulanmamış

Kanıt: [FirebaseSyncService.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebasesync/FirebaseSyncService.kt:398) initial pull yalnız yerelde yoksa insert ediyor; mevcut remote düzenlemeyi ve cihaz offline iken remote silinen kayıtları uzlaştırmıyor. timestamp yazılıyor ama sürüm kıyaslanmıyor. ADDED/MODIFIED/recentlyAdded zaman penceresi conflict policy değil.

Öneri: pending yerel değişiklikleri ezmeyen deterministic upsert/reconcile; deletion/version bilgisi ve açık conflict policy. Kör snapshot “yerelde yoksa sil” işlemi pending offline veriyi kaybettirir. Çok cihazlı tamamlamada client Mutex yeterli değildir; server transactional/idempotent sözleşme gerekir.

### R12 — P1: Room migration ve ID invariant'ları zayıf

Kanıt: [RoomModule.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/di/module/RoomModule.kt:35) fallbackToDestructiveMigration; database version13/exportSchema=false. Remote ID unique index yok; REPLACE local PK'ya çalışıyor, aynı remote ID farklı local ID ile çoğalabilir. Genel/kategori bütçede önce sorgula sonra insert yarışa açık.

Öneri: schema export, gerçek migration, dedup sonrası uygun unique/index; nullable category/general budget unique invariant'ını ayrıca tasarla. Mevcut sürümler/sahadaki veriler görülmeden fallback'ı silmek tek başına migration çözümü değildir. [Room migration rehberi](https://developer.android.com/training/data-storage/room/migrating-db-versions) destructive fallback'ın veri sildiğini açıklar.

## 4. Para, tarih ve finansal kurallar

### R13 — P1: para Double; para birimi kullanıcı diline göre değişiyor

Kanıt: entity amount Double; [FormatExtensions.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/utils/FormatExtensions.kt:7) Locale.getDefault ile currency formatter/symbol seçiyor. Bütçe alanı ise sabit ₺; AI prompt dilin para birimini kullanmasını istiyor (strings.xml284). Aynı kaydın locale değişince başka dövizmiş gibi sunulması mümkün; gerçek dönüşüm yok.

Öneri: ürün TRY tek para birimi ise açık TRY + Long kuruş; çok para birimi gerçekten gerekiyorsa currencyCode ile tutar ve kur politikası. Locale yalnız gösterim/ondalık biçimi. Room, Firestore, mapper, form, rapor ve mevcut veriler birlikte migrate edilmeli; sadece toLong çağrılarıyla geçilemez. BigDecimal parsing/yuvarlama gerektiği sınırda; tüm uygulamaya gereksiz decimal framework eklenmez.

### R14 — P1: tutar/yüzde doğrulaması ve düzenleme veri kaybı

Kanıt: AddVM201 yalnız boş/numeric kontrol; negatif/0/NaN/Infinity için tam sınır yok. Detail154 pozitif kontrol ekliyor ama NaN/locale/parsing eşdeğer değil. BudgetVM136 null parse sonucunu hata saymıyor, sonra 0'a düşürüyor; yüzde boşluk dışında aralık doğrulaması yok. BudgetVM218 tutarı toInt yaparak küsurat kaybediyor.

Öneri: aynı MoneyInputParser/validator; virgül/nokta, pozitiflik, finite, ölçek/üst sınır. Yüzde geçerli ürün aralığı (normalde >0 ve <=100) ve toplam yüzde davranışı açık olsun. Form immutable draft üzerinden doğrulansın; UI filtrelemesi domain validation yerine geçmez. Decimal keyboard, hatayı ilgili alanın altında göster.

### R15 — P1/P2: Home, Budget, AI aynı dönemi/kuralları kullanmıyor

Kanıt: HomeViewModel45 rolling previous month kullanıyor, HomeScreen81 “Bu Ayın Özeti” diyor. Budget current calendar month; AI repository tüm geçmiş masrafı aylık bütçeyle karşılaştırıyor. AI kategori yüzdesi için budget.amount kullanıyor, percentage limit hesabını yapmıyor ([AiRepositoryImpl.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/ai_repository/AiRepositoryImpl.kt:110)).

Öneri: FinancePeriod/DateRange modeli, ortak BudgetCalculator/FinancialSummary; aynı snapshot'tan Home/budget/AI projection. Gerekli kuralları pure Kotlin'e çıkar. Last month = önceki takvim ayı mı son bir ay mı karar ver; kaynak adı/etiket/dönem uyuşsun.

### R16 — P2: zaman kaynağı, date picker UTC ve stale range

Kanıt: DateFormatter Calendar/now, Home/Budget range ViewModel oluşturulurken sabitleniyor; gece/ay değişiminde aynı VM dönem yenilemez. DatePicker UTC millis ile local gün/şimdi karşılaştırılıyor. DateFormatter.formatRelativeDate gelecekteki bütün tarihlere “Bugün” diyebilir; today üst sınırı yok. Statik cache gün/timezone/locale değişiminde zor yönetiliyor.

Öneri: minSdk30 için java.time LocalDate/Instant/ZoneId ve inject Clock; date-only seçimle gerçek zamanlı dueAt ayrımı. DateRange [start,endExclusive) kullanımını DAO ile tutarlı seç. Gün değişimi/resume dönem tazelemesi; formatted string locale/config değişimine duyarlı. Gereksiz ThreadLocal/cache yerine küçük açık formatter.

### R17 — P2: yüzde adı ve gösterimi farklı anlamlar taşıyor

Kanıt: HomeViewModel148 calculateSpendingPercentage aslında (income-expense)/income yani kalan oran. HomeScreen182 negatif oranı full progress yapıp negatif yüzde yazıyor.

Öneri: remainingIncomeRatio veya gerçekten spentIncomeRatio; UI etiketini aynı anlama bağla. Gelir=0, negatif bakiye ve aşım durumunu açık göstermek; yalnız progress clamp finansal anlamı çözmez.

## 5. Planlanan işlemler, bildirim ve FCM

### R18 — P1: gelecekteki hatırlatıcı hemen tetiklenebiliyor

Kanıt: [AddTransactionViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/addtransaction/AddTransactionViewModel.kt:303) ve FirebaseSyncService473 ilk iş için 5 saniye; [NotificationWorker.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/notification/NotificationWorker.kt:50) yalnız currentTime<=endOfScheduledDay bakıyor, başlangıç/dueAt koşulu yok. Tüm pending kontrolü aslında tüm scheduled kayıtları geziyor.

Öneri: gerçek ilk hedef zamanından minimum delay; worker tekrar dueAt/status doğrulamalı. 15 dakikalık tekrar/snooze/sona erme ürün politikası olarak ayrı. WorkManager tam saat garantisi vermez; exact alarm gerçekten gerekmiyorsa ekleme. [WorkManager zamanlama](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).

### R19 — P1: scheduled → transaction aktarımı atomik/idempotent değil

Kanıt: [ScheduleViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/schedule/ScheduleViewModel.kt:38) insert ve delete ayrı; receiver aynı işin ikinci kopyasını içeriyor. Ekran yeni Firestore ID üretirken receiver scheduled ID'yi tekrar kullanıyor; medya taşıma ve cleanup yolları da farklı. Ekranda hızlı çift onay veya eşzamanlı receiver tekrarlı finansal kayıt doğurabilir.

Öneri: tek CompleteScheduledTransaction işlemi; Room transaction, unique completedFromScheduledId/operasyon kimliği ve durum geçişi. Server tarafında çok cihazlı duplicate completion için ayrıca atomik invariant. Reminder iptali, outbox ve medya devri aynı sözleşmeye bağlanmalı. Sadece shared UI değil, shared iş akışı gerekiyor.

### R20 — P1: “bildirimi silen” worker finansal kaydı siliyor

Kanıt: [DeleteExpiredNotification.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/notification/DeleteExpiredNotification.kt:26) remote ve local scheduled transaction siliyor; NotificationWorker 24 saat sonra enqueue ediyor.

Öneri: bildirim expiration ile finansal planın yaşam döngüsünü ayır. Kullanıcı onayı olmadan refactor sırasında bu ürün davranışını değiştirmeyelim; otomatik silme isteniyorsa açık adı/politikası/medya cleanup'ı, istenmiyorsa Expired durumu ve kaydın korunması. Geçici ağ hatası failure ile kalıcı kayıp olmaz; absent record çoğunlukla idempotent success.

### R21 — P1/P2: BroadcastReceiver işi onReceive sonrasında güvencesiz

Kanıt: [NotificationActionReceiver.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/notification/NotificationActionReceiver.kt:47) bağımsız scope.launch, goAsync yok. onReceive döndükten sonra süreç sonlandırılabilir.

Öneri: uzun/network işlem için account-aware kalıcı worker; kısa kritik local işlem için goAsync + finally finish ve süre sınırı. [Android broadcast rehberi](https://developer.android.com/develop/background-work/background-tasks/broadcasts). Notification'ı gerçek işlem kaydedilmeden kapatmanın UX sonucu da ele alınmalı.

### R22 — P2: yinelenen zamanlama ve iptal anahtarları

Kanıt: ilk Add işinde unique work var; sync ve tekrar işler bazı yerlerde yalnız enqueue/tag kullanıyor. Local işler, FCM ve server reminder birden fazla tetik kaynağı. Aynı record/version/event için kalıcı dedupe yok.

Öneri: küçük ReminderScheduler, ortak work name/tag/input constants; account+record bazlı unique policy. Tamamlama/silmede bütün ilgili zinciri iptal et. Olay dedupe/last delivered durumu gerekiyor; rastgele hash ID sadece notification slotunu değiştirir, işin tekrarını önlemez. FCM teslimi/WorkManager zamanı için garanti vaat etme.

### R23 — P1/P2: bildirim izni ve içerik gizliliği

Manifest POST_NOTIFICATIONS var, üretim kodunda runtime request ve izin reddi akışı yok. Worker doğrudan notify ediyor; denied durumda worker exception/failure veya görünmeyen reminder oluşabilir. Hatırlatıcı metni finansal tutar/kategori taşıyor; lockscreen privacy varsayılanına bırakılmış.

Öneri: Android13+ izin isteyen anlamlı UI (reminder açarken), reddi/settings'e gidiş, worker'da permission/channel-aware güvenli no-op/policy. Kilit ekranında private/publicVersion tercihi. İşlem adlarında kullanılabilir stringResource kategori eşlemesi; enum.name İngilizce kodu kullanıcıya gösterme.

### R24 — P2: FCM sözleşmesi, token ve servis yaşam süresi

Kanıt: [MyFirebaseMessagingService.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/fcm/MyFirebaseMessagingService.kt:43) onNewToken verilen token'ı kullanmıyor, yeniden fetch; scope onDestroy'da iptal edilmiyor. userId gelmezse payload kabul ediliyor. Server sorgusu/yerel kayıt yokluğunda gelen reminder kayboluyor; mesaj kalıcı iş olarak izlenmiyor.

TokenManager hata yutuyor. Logout offline token revoke tamamlanmadığında eski token ilişkisi kalabilir; local currentUser guard yalnız uygulama tarafından işlenen mesajlar için koruma.

Öneri: parsed typed payload + schema validation, eventId, account match ve idempotent işlem; kısa callback'ten dayanıklı işe devir. token upsert/revoke observable/retry; server eski token temizliği/kimlik kontrolü ayrıca doğrulanmalı. Supplied token, request-generation ve account-bound scope.

### R25 — P2: FCMNotificationSender adı gerçek görevini anlatmıyor

Kanıt: [FCMNotificationSender.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/fcm/FCMNotificationSender.kt:15) FCM göndermiyor; Firestore reminder silip yeni reminder ekliyor. Silme ve create iki farklı commit; iki cihaz aynı anda snooze ederse duplicate oluşabilir. ACTION_CANCEL aslında tekrar hatırlatma; bildirim dismiss de aynı eyleme bağlanmış.

Öneri: ReminderRemoteDataSource/SnoozeReminderRepository; deterministic reminder ID veya transactional server intent. Cancel/Snooze/Dismiss ayrımı. Sunucu FCM gönderimi ayrı; client'a service-account veya server key koyma.

## 6. Fotoğraf, konum, kaynak yönetimi

### R26 — P1: fotoğraf upload işi belge oluşmadan başarılı bitiyor

Kanıt: AddVM225–239 upload'ı local/remote kayıttan önce enqueue ediyor; [PhotoUploadWorker.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/photo/PhotoUploadWorker.kt:36) remote belge yoksa success. Geçici “henüz oluşturulmadı” ile “kullanıcı sildi” ayırt edilmiyor.

Öneri: local kayıt + pending metadata, remote upsert → photo upload → reference attach sırası; stable id/version ve pending delete kontrolü. Sadece belgesizse sonsuz retry değil; gerçekten silinmişse cleanup/success. Upload/update hata ayrımı.

### R27 — P1: fotoğraf değiştirme başarısızsa önceki fotoğraf kayboluyor

Kanıt: [DetailViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/history/DetailViewModel.kt:78) önce eski dosyayı siliyor, sonra yenisini kaydediyor; null hata kullanıcıya açıklanmıyor. AddVM catch DB kaydı oluşmuş olsa bile savedPhotoPath silebiliyor.

Öneri: yeni fotoğrafı atomic/temporary save, DB referansını değiştir, ardından kullanılmayan eski dosyayı temizle. Local commit sonrası scheduling hatası local veriyi rollback etmeyen ayrı sonuç olmalı. Worker kullandığı dosya upload bitene kadar korunmalı.

### R28 — P2: bitmap/dosya işlemleri main thread ve bellek riski

Kanıt: [PhotoStorageUtil.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/photo/PhotoStorageUtil.kt:19) synchronous IO + full decode ardından resize; VM main scope'tan çağırıyor. Stream close finally/use değil; EXIF orientation yok; camera dosyası resize yapmadan kopyalanıyor.

Öneri: inject PhotoLocalStore, main-safe suspend; sampled decode/boyut sınırı/EXIF/atomic write; dosya IO dispatcher, ağır CPU işinin gerekiyorsa Default. Küçük UUID/file-path oluşturma için otomatik coroutine gerekmiyor. Medya durumunu Uri/path/remote storage ref olarak ayır.

### R29 — P1/P2: indirme yarışları ve disk birikimi

Kanıt: FirebaseSyncService445 async download insert'ten önce başlıyor; updateEntityWithPhoto kayıt yoksa geçiyor. 652 remote URL'yi local path ile kıyaslıyor; eşitlik beklenemez, tekrar indirip yeni dosyalar oluşturabilir. PhotoStorageManager SYNC dosyaları, signOut/remote delete/replacement cleanup eksik.

Öneri: local kayıt önce; remotePhotoVersion/storagePath ve local cache ayrı; deterministic filename/hash, account directory, download dedupe, referenced/pending dosyayı koruyan cleanup. Coroutine upload/download her aşamada aktif hesap kontrolü yapmalı. Cihazdaki önceki insufficient storage hatasının nedeni olarak bu kodu kanıtlanmış saymıyoruz; bu ayrı statik disk büyümesi riski.

### R30 — P2: Camera launcher ViewModel'de tutuluyor, permission akışları farklı

Kanıt: AddTransactionScreen73 CameraHelper'ı oluşturup viewModel.cameraHelperRef'e yazıyor; Activity Context/launcher UI ömründen uzun tutulabilir. Detail permission callback yalnız Toast gösteriyor, CameraHelper.onPermissionResult'ı çağırmıyor; izin verilince capture otomatik devam etmiyor. Cancelled camera temp cleanup aynı değil.

Öneri: launcher/helper route içinde; VM yalnız draft/media sonucu alır. ActivityResultContracts.TakePicture + dar FileProvider bu uygulama için yeterli; CameraX eklenmesi gerekmiyor. Gallery için mevcut GetContent geçerli; PickVisualMedia güncel, ölçülü UX seçeneği. CAMERA izni dış kamera intent'i için gerçekten gerekli mi ayrıca değerlendirilip manifest/request birlikte temizlenmeli.

### R31 — P2: location erişimi, cancellation ve selected marker

Kanıt: [LocationUtil.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/location/LocationUtil.kt:30) Android30–32 synchronous Geocoder; UI/VM coroutine main üzerinde çağırabiliyor. Android33 async callback onError işlenmiyor, continuation/cancel sözleşmesi eksik. Fused callback viewModelScope içinde başlatılıyor ama beklenmediği için scope iş bitirmiş sayılabilir; CancellationTokenSource cancel bağlanmıyor.

UI coarse veya fine kabul ederken init/resume yalnız fine bakıyor. selectLocation ardışık istekleri iptal etmiyor; eski adres yeni seçimi geç yazabilir. Map marker remember(initialPosition) seçilen yeni konuma açık şekilde güncellenmiyor. errorMessage UI'da gösterilmiyor; başarısız arama sessiz.

Öneri: LocationGateway/AddressResolver dar DI sınırı, main-safe eski API; cancellable task awaiting, onError ve terminal callback; latest search/geocode job + coordinates token; coarse/fine tutarlı. Marker için selected state ve drag snapshot akışını tek yönlü bağla; shared ui state'e query/search/error dahil et. Map UI, permission launcher ve settings açma route'a ait.

## 7. MVVM, SOLID ve ölçülü hedef mimari

### R32 — P2: katmanlar SDK ve UI tipleriyle birbirine bağlı

Kanıt: AuthRepository AuthResult/GoogleSignInAccount; Finance/Budget/Ai repository Room Entity döndürüyor. AuthRepositoryImpl import screens.auth.AuthException. ViewModel'ler FirebaseSyncService/WorkManager/SDK'ları somut kullanıyor; AI data katmanı UI category resource mapper'ına bağlı.

Hedef bağımlılık: presentation → domain ← data; DI bunları birleştirir. AuthResult yerine gerekli AppUser/Session bilgisi, Google UI sonucundan token payload; Room entity ve Firestore DTO data içinde, mapper açık ve tür bazlı. Domain'de Android Context/R/SDK yok. Repository içinde çok adımlı kayıt/sync sınırını sahiplenmek, UI'a ayrı local+cloud koordinasyonu bırakmamak.

SOLID tek tek:

- S: SyncService; AI model/rapor/chat DB/cloud birleşimi; AuthVM'nin dört ayrı formu; shared completion akışı.
- O: sync türlerinin mapper/remote/local operasyonları ayrıldığında ekleme küçük ve anlaşılır olur. Her türe generic adapter registration altyapısı zorunlu değil.
- L: interface implementasyonlarında belirgin Liskov ihlali kanıtı yok; “illaki tüm harflerde sorun var” demiyoruz. Result/suspend/cancel/id sözleşmesi netleştirilmeli.
- I: FinanceRepository transaction/schedule/read aggregation'ı karıştırıyor; gerçek tüketiciye göre TransactionRepository ve ScheduledTransactionRepository ayrımı düşünülebilir. Tek metotlu onlarca interface üretmeyelim.
- D: data → UI import ters; VM → concrete FirebaseSyncService de bağımlılık sınırını zayıflatıyor. Değişebilir dış servis/saat/iş scheduling sınırlarında DI.

### R33 — P2: çok sorumluluklu iki ana sınıfın bölünmesi

FirebaseSyncService: oturum lifecycle, kimlik üretimi, DTO mapper, pull/push, listener, DB mutation, fotoğraf, bildirim, Functions call. Öneri: SessionSyncCoordinator + ilgili record mapper/data-source'lar + OutboxWorker + MediaCoordinator + ReminderScheduler. Bu iş sahipliğine göre ayrımdır; hepsini aynı generic SyncManager altında saklamayalım.

AiRepositoryImpl: model çağrısı, rapor/finans hesabı, prompt render, chat DB/cloud ve hatalar. Öneri: AiClient, FinancialReportBuilder/shared financial snapshot, ChatRepository. Pure helper için interface şart değil; dış model client için anlamlı.

### R34 — P2: feature-first paketlerin konum ve sahiplik planı

Tek :app modülünde önce aşağıdaki yön yeterli; bu bir hedef taslak, henüz taşıma yapılmadı:

```text
app/                         application, DI, uygulama navigation/shell/session
core/database/               ortak DB tanımı, migration, gerçek shared DB altyapısı
core/ui/                     theme ve birden fazla feature'ın gerçek ortak UI'ı
core/time/                   Clock ve ortak dönem hesabı
core/media/                  iki işlem ekranının kullandığı fotoğraf altyapısı
core/sync/                   outbox/session sync altyapısı
feature/auth/
  domain/ data/
  presentation/signin/
  presentation/signup/
  presentation/passwordreset/
feature/transaction/
  domain/ data/
  presentation/add/
  presentation/history/
  presentation/detail/
feature/schedule/
  domain/ data/ presentation/
feature/budget/
  domain/ data/ presentation/components/
feature/home/presentation/
feature/aichat/
  domain/ data/ presentation/
feature/location/
  data/ presentation/
```

Domain modelleri onları yöneten feature'a ait olmalı; örneğin Transaction domain modelini home/AI tüketebilir. “Ortak kullanılıyor” diye bütün domain'i core/model çuvalına taşımayalım. DAO/entity sahipliği feature data/local altında, gerçek ortak DB composition core/database altında olabilir. Notification server/client orchestration schedule feature'ında; genel notification builder yalnız gerçekten genel ise shared altyapı.

Şimdiki feature paketi feature-first değildir; çoğunlukla navigation içeriyor. screens/main yapısı görünüm yerleşimini, roomrepository/firebaserepo ise teknoloji seçimini esas alıyor. Bunlar sahipliğe göre taşınmalı. Home küçük kalabilir; data/domain klasörleri içerik yoksa boş yaratılmaz. location iki ekran tarafından kullanılıyor diye her dosya utils içine atılmaz.

### R35 — P2/P3: adlandırma eşlemeleri ve kod stili

| Şimdiki | Öneri / gerekçe |
|---|---|
| roomdb/entitiy | entity veya feature/data/local/entity; yazım hatası |
| budgetrepositroy | budgetrepository geçici düzeltme; nihai feature/budget/data |
| DataPickerField.kt / DatePickerField | DatePickerField.kt |
| isRemenderEnabled | isReminderEnabled |
| Screens enum karışık UPPER/PascalCase | tek route modeli/konvansiyon; string route compatibility koru |
| Screens.AnalysisScreen (Budget destination'ı) | BudgetRoute/BudgetScreen |
| DetailScreen/DetailViewModel | TransactionDetailScreen/TransactionDetailViewModel |
| AiViewModel | AiChatViewModel |
| ScheduleViewModel | ScheduledTransactionsViewModel |
| MyFirebaseMessagingService | FinanceMessagingService; görevi belli |
| FCMNotificationSender | ReminderRemoteDataSource/SnoozeReminderRepository |
| DeleteExpiredNotification | gerçek davranışa göre ExpireReminderWorker veya DeleteExpiredScheduledTransactionWorker |
| PhotoHelper dosyası / CameraHelper class | CameraCaptureLauncher/CameraHelper; route helper sahipliği |
| EditTextField/EditButton/EditTopBar/EditAlertDialog | FinanceTextField/FilterButton/FinanceTopAppBar/FinanceAlertDialog gibi gerçek rol |
| BudgetDao() database metodu | budgetDao() |
| getAll...(): Flow | observeTransactions/observeBudgets; suspend one-shot için get |
| repo/context/type gibi belirsiz kısaltmalar | private transactionRepository, applicationContext, transactionType; bağlam açık küçük lambda'da it kabul |
| FinanceNavigation.kt package yok | app.navigation package; default package import kaldır |
| bottomNavItem koleksiyonu | bottomNavItems; pozisyon index==2 yerine item rolü |
| topBarTitleForRoute @Composable | saf @StringRes fonksiyon; route metadata sahibi shell |

PascalCase composable, lowerCamelCase normal fonksiyon, private constant UPPER_SNAKE; modifier ilk optional parametre olarak tek konvansiyon. Wildcard/import format, gereksiz @OptIn/import, unused state/resource temizliği P3. Sadece satır sayısını azalttığı için anlamlı isimden vazgeçme. Room enum persist name/Firestore alan ve route string'lerini rastgele yeniden adlandırmak veri/link kırabilir; presentation adları önce, serialized değerler kontrollü migration ile.

## 8. Ekran bazında parçalama ve state önerileri

| Ekran / dosya | Somut karar |
|---|---|
| SignInScreen (293 satır) | SignInRoute launcher/state/effect; stateless SignInScreen; AuthScaffold ve shared PasswordField. Callback yalnız gerekli eylemleri taşımalı. |
| SignUpScreen (283) | Route + form + verification dialog; dialog'a hiltViewModel verilmez, state/callback verilir. SignUpUiState ve yerel doğrulama. |
| PasswordResetRequestScreen (201) | Request form ve sonucu; ad/soyad ile user sorgusu ürün/gizlilik kararı, sade email-reset tercihinin onayı gerekir. |
| PasswordResetScreen (216) | Link code durumları/loading/expired/invalid; tekrar eden state reset/dialog temizliği. |
| AddTransactionScreen (498) | Route (launcher/navigation/feedback) + TransactionForm + attachments/location/photo + date dialog. VM'den mutable UI flag'lerini set etmek yerine named actions. |
| DetailScreen (535) | TransactionDetailRoute + DetailContent + EditTransactionSheet + PhotoViewerDialog + PhotoCapture route. EditBottomSheet ViewModel değil edit state/callback alır. Loading/NotFound/Error ayrımı. |
| TransactionHistoryScreen (281) | HistoryRoute + Filters + TransactionList/Card; scheduled sekmesi için shell ve feature-owned Route ayrımı. NavController kartın içinde değil onTransactionClick(id). |
| ScheduledTransactionScreen (218) | Route/list/item zaten kısmen ayrılmış; UI state'e in-progress ID ve completion error ekle. Küçük item fonksiyonu başka dosyaya taşımak zorunlu değil. |
| HomeScreen (203) | SummaryCard/SuggestionCard/ExpenseChart; Home route gereksiz AiViewModel oluşturmasın; prompt navigation argument/intent. Shell için ayrı account state; iki HomeVM yerine sorumluluk ayır. |
| AiChatScreen (307) | Zaten MessageList/Bubble/Input/Suggestion olarak bölünmüş; hepsini tekrar yazma. Route/content sınırı, model UI data, private görünürlük, send/loading/error/retry ve list keys. |
| BudgetScreen (82) | İnce state yönlendirme iyi; loading empty karışmasını düzelt, budget delete mesajı transaction demesin. |
| AddBudgetBottomSheet (271) | Yerel selector/textfield parçaları mantıklı. Parçaları budget/components altında gerektiğinde ayır; save guard/validation/keyboard. |
| EmptyBudgetContent (193) | Ortak monthly budget card ile Filled'deki empty card gerçek tekrar; empty içeriği aynı feature'da tut. |
| FilledBudgetContent (520) | Zaten card/list/helper fonksiyonları var. BudgetSummaryCard/CategoryBudgetCard/WarningCard'a dosya ayrımı okunabilirlik sağlar; yeni generic kart framework gerekmez. |
| SmoothLinearProgress (36) | Küçük ve feature-yerel kalabilir. “Smooth” animasyon yapmıyor; isim düzelt veya ihtiyaç varsa animasyon; erişilebilir progress semantics ekle. |
| MapLocationPickerScreen (313) | Route/permissions/settings + MapContent/SearchBar; query/search/geocode VM sahibine, camera animation UI scope'a. |
| SplashScreen (51) | Finansal session kararını composable dışına taşı; yapay 3 saniye zorunluluğu kaldır; UI küçük kalabilir. |

### R36 — P2: public mutable form ve tutarsız state kaynakları

Add/Detail/Ai/History/Location ekranları VM alanlarını doğrudan değiştiriyor. Auth çoğu input'ta private set kullanıyor; bu iyi korunmalı. BudgetUiState/formState/deleteDialogState anlamlı ayrımlar; tek nesneye zorla yığma.

Öneri: immutable screen ui state veya private-set Compose state + named handlers; aynı mantık için tek kaynak. History _filterTrigger integer ile Compose fields'i tetikliyor; MutableStateFlow<TransactionFilter>.flatMapLatest daha açık. isHistoryPage sekme/selected filters için SavedStateHandle/rememberSaveable ihtiyaca göre. Kaydetmede draft snapshot al, isSaving guard; await sonrası mutable form yeniden okunmasın. Bütçe eşzamanlı save/conflict DB invariant'ıyla korunmalı.

### R37 — P2: Flow zinciri, loading/error ve stateIn yerleşimi

Home aynı income/expense query'lerini farklı StateFlow'larda yeniden kurup combine ediyor. Snapshot alanları geçici olarak farklı güncellemelerden gelebilir; tek aggregate read/projection daha tutarlı. UI'da emptyList initialValue gerçek boş veriyle loading'i ayırt etmiyor; Detail null da Loading/NotFound karışımı.

Room Flow distinctUntilChanged sonucu değişmediyse redraw önlemek için anlamlıdır; repository ve VM'de eşdeğer yinelenen distinct/flowOn gözden geçirilmeli. Flow catch/retry UI state üretmeli; exception sessizce collector öldürmemeli.

UI'a sunulan cold akışta stateIn(viewModelScope, WhileSubscribed(5_000), initialState) uygun. Direkt MutableStateFlow.asStateFlow için tekrar stateIn gerekmez. Finans sync'i/kalıcı iş UI subscription'a bağlanmamalı. Mevcut lifecycle collection korunacak. [Android mimari önerileri](https://developer.android.com/topic/architecture/recommendations).

### R38 — P2: UI eylem sonuçları ve navigation tutarlılığı

AuthState SUCCESS/FAILURE birden fazla eylemin sinyali; LaunchedEffect reset/toast/navigation tekrarı var. saveTransaction callback ve diğer VM event yüzeyleri farklı; hepsini tek dev event bus'a çevirmeyelim.

Öneri: operasyonun sonucunu screen state olarak tut, işlenen mesaj/result ID ile tüketimi açık yap. Navigation kullanıcı eylemi sonucu UI sınırında; request/save sonucu process/lifecycle değişiminde kaybolmamalı. SharedFlow/Channel tek başına kritik başarı garantisi değildir. Password/reset/verification ayrı sonuç semantiği.

### R39 — P2/P3: parametre sayısı için doğru ölçüt

EditTextField'ın 13 parametresi var ama çoğu TextField konfigurasyonu; otomatik “parametre torbası” oluşturmak okunabilirliği düşürür. Material slot/callback API'si korunabilir; optional modifier konumu, enabled/isError ve form ihtiyacını düzeltmek daha yararlı.

TransactionEntity/Scheduled entity constructor'ında amount/category/note/time/photo/location flat tekrar var: DTO/entity ile birlikte TransactionDraft ve LocationData/PhotoState anlamlı gruplar; storage şemasını sırf constructor kısa olsun diye denormalize/serialize etmeyelim. Sync/AI 8/7 bağımlılıklarını Dependencies bag'e koymak SOLID çözümü değil; sahiplik bölünmeli.

Function reference stili sadece düz forwarding: onValueChange = viewModel::updateInputAmount; iki adım, enum argümanı, domain event oluşturma veya navigation varsa lambda. “viewModel.login” fonksiyonu çağırmaz; callback olarak fonksiyon referansı ::, çağrı olarak login() kullanılır.

## 9. Coroutine, dispatcher ve cancellation politikası

### R40 — P1/P2: catch(Exception) cancellation'ı da yutuyor

Sync/media/auth/AI/worker/location çok sayıda catch(Exception), empty catch ve Result.failure dönüşü içeriyor. CancellationException da Exception; iptali sıradan kullanıcı/ağ hatasına çevirmek eski oturum işlerini sürdürebilir, yanlış retry/error satırı üretebilir.

Öneri: cancellation'ı rethrow; beklenen hata kategorilerini açık ele al; finally kaynak cleanup. Firebase Task .await kullanılmalı (sendPendingNotifications callable sonucu şu anda await edilmiyor). Sistemin cache/SDK işleri main-safe ise sırf “ağ” diye withContext(IO) ekleme. [Android coroutine iyi uygulamaları](https://developer.android.com/kotlin/coroutines/coroutines-best-practices).

Scope sahipliği:

- viewModelScope: screen state üretimi ve ekrana ait talep; kalıcı commit/outbox repository'de.
- rememberCoroutineScope: UI animation/sheet/camera gibi composable ömrüne bağlı işler; finans kaydı garantisi için değil.
- account session scope: dinleyici/oturum koordinasyonu; UID boundary/reset/cancel yönetimli.
- WorkManager: process death/reboot/ağ tekrarına dayanıklı pending sync/media/reminder işi; DB niyeti kalıcı.
- receiver/service callback: kısa parse/devir; sınırsız bağımsız scope yeni oluşturma.
- IO: dosya, blocking Geocoder, synchronous database clear gibi blocking işlem.
- Default: gerçekten ağır CPU rapor/bitmap işleri; Main: hızlı state/UI.
- Suspend Room DAO ve Room Flow zaten asenkron çalışıyor; ek IO wrapper'ı sadece başka blocking iş varsa. [Room asenkron DAO](https://developer.android.com/training/data-storage/room/async-queries).

Mevcut IoDispatcher DI olumlu. DefaultDispatcher/Clock yalnız gereken yerde eklenir. Tüm dispatcher'ları config paketi tek “thread manager” altında gizlemeyelim. Hilt @Provides kullanılan her basit binding'i sırf moda diye @Binds'a çevirmek gerekmez; ikisi de geçerli.

## 10. Metin, hata, tasarım tutarlılığı ve erişilebilirlik

### R41 — P2: hata sözlüğü ve kaynak sahipliği

Çoğu ekran metni XML'de; HomeViewModel önerileri/prompt'ları kodda. Kaynaklar error_invalid_amount/invalid_amount/error_enter_valid_amount ve select_category_error/error_select_category gibi yineleniyor; error_not_logged_in yalnız “Bir şeyler ters gitti”. Failure string'e exception argümanı verilip format suppression uygulanmış. Budget silme dialog'u transaction metnini kullanıyor. Konum kaldırma ikonunda remove_photo metni var.

Öneri: temel shared action/validation strings; feature'e özel meaningful resource prefix; bütün 292 satırlık strings.xml'i bölmek tek başına gerekmez, auth_strings/transaction_strings gibi feature sahipliğinde bölebiliriz. Kaynak isimleri snake_case; error_firstName/sign_in_with_Google/email_diaolog yazım/konvansiyon düzeltmeleri. UI mesajı Türkçe tutarlı, eylem odaklı. İlgili “aynı anlam” ortaklaştırılır; reset/link/network/auth farklı hataları tek generic cümlede kaybetme.

Data/domain Android string ID dönmek zorunda değil: minimal typed AuthFailure/SaveFailure; presentation mapper → resource/format args. Android UI StringRes annotation, List<Any> warning args yerine typed budget warning. Log teknik exception'ı korur ama email/token/finans içeriği basmaz. Kullanıcıya e.message/e.localizedMessage göstermek veya normal AI mesajı diye kaydetmek yok.

### R42 — P2: tema yüzeyi üç farklı kaynaktan yönetiliyor

Theme dynamic/light scheme kullanıyor; ekranlar sabit koyu XML background/hex ve onPrimary ile metin çiziyor. onPrimary her yüzeyin yazı rengi değildir; dynamic/light durumda kontrast bozulabilir. Color.kt starter palette, renkler birçok ekranda tekrar ediyor.

Öneri: ürün dark-only mı light/dynamic de desteklenecek mi karar; sonra background/surface/onSurface/error semantic color. Birkaç gerçek ortak gradient/spacing token; her dp için constant sınıfı yok. TextField styles feature yerine theme'ye ait gerçekten shared yüzeyden beslensin. Görsel tasarımı değiştirmeden kontrast düzeltmeleri ayrıca ölçülmeli; bu tur görsel QA yapılmadı.

### R43 — P2: erişilebilirlik ve responsive UI

Kanıt: action icon'larda null contentDescription (logout/back/map search clear/send/budget edit/delete), 20/24/32dp icon action yüzeyleri, BasicTextField input'ın açıklaması yok. Dekoratif icon'da null doğru; etiketli Button ikonunda ayrıca tekrar description gerekli değil.

Auth reset ekranlarında top240dp/width280 gibi sabit yerleşim; Detail sheet scrolling/IME yönetimi sınırlı. Add/root Modifier çocuklarda tekrar kullanılıyor: dışarıdan gelen padding/weight/semantics alt ağaca yeniden uygulanabilir. Chart nativeCanvas text pixel36/etiketler, responsive/font scale/RTL/accessibility data açıklaması yok; her draw için Paint/color list üretimi. SmoothLinearProgress progress semantics yok.

Öneri: root modifier yalnız root, çocuk Modifier; meaningful action labels/touch target; field validation isError/supportingText/imePassword-Decimal-email; IME/insets ve widthIn; grafik için erişilebilir kategori/tutar metni/legend, küçük ekranda etiket çakışmasını önle. Dark/light font scale TalkBack cihaz kontrolü son doğrulamada.

### R44 — P2/P3: UI performansı ve state restorasyonu

History/Ai Lazy items stable key eksik; scheduled list bunu doğru yapıyor. AI otomatik scroll kullanıcı geçmiş okurken her yükleme/gönderimde aşağı çekiyor; target index'i mevcut son item/loading satırıyla tutarlı olsun. remember(messages) initial localized greeting'i locale key olmadan tutuyor; chart localized cache de config'e duyarlı değil.

Ai pendingAutoPrompt companion global: hesap/screen arası sızıntı ve process death kaybı. Home AI navigation arg/intent, SavedStateHandle veya tek graph-scoped açık sahibi kullan; global değişken yok. Persisted finansal state DB'de, draft/query gerektiği kadar saved state'de, bitmap/Context saved state'de değil. Chat/history için veri büyüdüğünde pagination değerlendirilir; ölçüm olmadan Paging zorunlu tutulmaz.

BackHandler {} SignIn/Home'da tüm geri davranışını tüketiyor; auth/root navigation temizliğiyle doğru back stack ve normal çıkış davranışı. Stateless içerikler preview edilebilir; “Preview yok” tek başına P1 hata değil.

## 11. Manifest, deep link, backup ve build

### R45 — P1/P2: password-reset deep link akışı eksik

Kanıt: Manifest financeai://resetPassword tanımlı; AuthNavGraph reset composable oobCode alıyor ama bu route için navDeepLink/intent parser yok. HandleDeepLinks yalnız main/schedule ele alıyor; MainActivity.onNewIntent yalnız setIntent yapıyor. Sıcak intent lifecycle resume olmadan gelirse handling ayrıca güvence altında değil.

Öneri: tek app-level DeepLinkParser; scheme/host/path/action/oobCode doğrulama, cold/warm intent ve oturum bekleme sonrası tek tüketim. Reset link SIGNED_OUT'ta da yönlenmeli; scheduled link verified/ready oturuma gated olmalı. App Links yalnız sahip olunan HTTPS domain varsa; keyfi domain üretme. Custom scheme başka app tarafından da tutulabilir; internal notification intent MainActivity'ye explicit bağlanmalı.

### R46 — P1/P2: back stack ve route string'leri

navigateSingleTopClear popUpTo(0) kullanıyor; tüm tab geçişleri stack/state'i siliyor. SignOut ana grafiği root'tan inclusive temizlemeden SignIn'a gidiyor; eski authenticated graph tutuluyor, UI back handler bunu örtüyor. TransactionCard hardcoded Detail_Screen/id, enum ve MainNavConstants aynı route'u tekrar ediyor.

Öneri: auth boundary geçişinde eski graph'ı temizle; tablar için graph start destination + saveState/restoreState stratejisi (ürün tercihi). Route construction/args tek kaynak. DetailViewModel argümanı zaten Int okuyor; sorun string anahtar ve eksik argümanda sessiz 0 fallback. Kullanıcının açık isteğiyle Navigation Compose type-safe route geçişi refactor hedefidir: SavedStateHandle.toRoute ile typed argüman ve geçersiz/bulunamayan işlem state'i. Navigation3 bu geçiş için gerekli değil; ayrıntılı dosya kapsamı bölüm 17'de.

### R47 — P2: Manifest izin ve başlangıç yapılandırması

FileProvider exported=false/grantUriPermissions ve dar transaction_photos path olumlu; kamerayı required=false tanımlamak da doğru. FCM service/receiver exported=false ve immutable PendingIntent mevcut; korunmalı.

SCHEDULE_EXACT_ALARM üretim kodunda AlarmManager kullanımına karşılık gelmiyor; mevcut reminder WorkManager ile approximate. Kullanılmıyorsa kaldırılmalı. RECEIVE_BOOT_COMPLETED/WAKE_LOCK WorkManager library manifestinin ihtiyacı olabilir; sadece “kodda import yok” diye topluca silinmez. ACCESS_NETWORK_STATE de merged manifest/bağımlılık sahibi kontrolüyle ele alınır.

InitializationProvider tümü tools:node=remove; yalnız WorkManager default initializer kaldırmak daha dar kapsamlı olabilir. Diğer Startup initializer'larını yanlışlıkla kaldırmamak için metadata-level removal. Mevcut HiltWorkerFactory + Configuration.Provider temelini koru; merged manifest/runtime kontrolü bu tur yapılmadı. Application DI package altında değil app başlangıç sahibi; kullanılmayan injected WorkManager/import temizliği.

### R48 — P1/P2: backup ve anahtar/gizlilik sınırları

allowBackup=true; backup_rules/data_extraction_rules örnek/boş. Account sahipliği olmadan restore edilmiş DB/fotoğraflar yeni oturumda görünme riski artar. Backup tercihi bilinçli olmalı: finans DB/media, auth prefs/token, cihazlararası restore ve pending ops ayrı ele alınır. “Her şeyi kapat” otomatik zorunlu değil.

Maps key Manifest'te hardcoded. local.properties/manifest placeholder kaynak hijyeni sağlar, APK anahtarı gizlemez. Android package+SHA ve API restriction Console'da doğrulanmalı; restrictions şu an görülmedi. [Maps güvenlik rehberi](https://developers.google.com/maps/api-security-best-practices).

Gradle, GEMINI_API_KEY sağlanırsa onu BuildConfig üzerinden APK'ya gömecek şekilde yazılmış. Mevcut local.properties içinde bu property yok; mevcut üretilmiş debug BuildConfig değeri de boş. Dolayısıyla şu snapshot için Gemini anahtarı sızmış demiyoruz; boş config ve eklendiğinde APK'ya gömme tasarımı iki ayrı sorundur. local.properties gitignored olması runtime güvenliği sağlamaz. Firebase AI Logic + App Check veya authenticated/rate-limited server proxy önerisi. Firestore/Storage owner rules ve App Check ayrıca sürüm kontrollü/server kapsamına alınmalı. google-services.json bir client config; service account private key ile aynı sınıfta “secret” sayılmaz.

### R49 — P2: Gradle bildirimi iki kaynak ve sürüm hizası

Kanıt: [build.gradle.kts](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/build.gradle.kts:54), [libs.versions.toml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/gradle/libs.versions.toml:1). Room2.8.0 explicit compiler/runtime/ktx + catalog room-ktx2.8.3; Activity1.8.2 + catalog1.11; Material3 explicit1.3+BOM alias; catalog duplicate credentials/hilt/firebase version aliases. Firebase BOM34.2 yanında messaging23.4/functions20.4 explicit versiyonları. Gradle hangi sürümü resolve etti bu tur çalıştırılmadı; “eski sürüm kesin kullanılıyor” veya “kesin build bozuk” demiyoruz.

Öneri: tek version catalog, tek alias/artifact; Room compiler/runtime hizası, Compose BOM kapsamındakileri versiyonsuz, Firebase BOM ile gereksiz pin kaldır. Firebase KTX alias'ları kullanım yoksa temizle; artık main module KTX API kullanılmalı. [Firebase BOM/KTX rehberi](https://firebase.google.com/docs/android/learn-more).

Places dependency import/kullanım yok; actual location Geocoder/Maps. Credentials/googleid declared fakat eski GoogleSignIn akışı kullanılıyor; migration yapıp sonra unused temizle. lifecycle-runtime-compose ve coroutines-play-services üretim imports'ları için açık direct dependency gerekebilir; transitive dependency'ye yaslanma. material-icons-extended büyük yüzey, kullanılan küçük icon/vector set değerlendirilebilir; sırf yeni diye her icon değiştirilmez.

Kotlin2.0/KSP eşleşmesi mevcut; AGP8.13/Gradle8.13 tanımlı. Güncelleme gerekiyorsa resmi compatibility matrix ve değişiklik notlarıyla ayrı kontrollü adım; “bütün version'ları latest yap” önerilmiyor. JVM target11 ile IDE JDK21 farklı amaçlar içindir; tek başına yanlış değil. Gradle wrapper executable bit eksik; wrapper doğrulama/checksum/yenileme tooling temizlik konusu.

### R50 — P2: eski Google/Gemini SDK yolunu güncelle

Legacy GoogleSignIn deprecated; mevcut Credential Manager dependency'leriyle Sign in with Google akışını modernize etmek anlamlı. [Resmî Google geçiş rehberi](https://developer.android.com/identity/sign-in/legacy-gsi-migration).

Google AI mobile generativeai SDK deprecated; Firebase AI Logic geçişi bu Firebase ağırlıklı uygulamada ölçülü seçenek. API key'i APK'dan çıkarır, App Check güvenlik ekler; yetkilendirme/kota/privacy hâlâ ayrı korunur. [Resmî AI SDK geçiş rehberi](https://firebase.google.com/docs/ai-logic/migrate-from-google-ai-client-sdks).

CameraX mevcut capture için yok/gerekmiyor. Coil/Material3/Room/Hilt/WorkManager kalabilir. Coil3/Navigation3/KSP2 gibi büyük geçişlerin her biri ayrı ihtiyaç/compatibility değerlendirmesi; bu refactor'a sırf CV teknolojisi olsun diye eklenmez.

### R51 — P2/P3: release ve işletim görünürlüğü

release minify=false; resource shrink yok. Ölçülü R8/shrink release adımı düşünülebilir, ProGuard broad credentials/SDK keep kuralları gerçek ihtiyaçla sınırlanır. Obfuscation API key'i güvenli yapmaz. Debug/release Firebase/SHA/backend environment ayrımı dokümante edilmeli; appId değişikliği ürün/link/data compatibility ile ele alınır.

Empty catch/printStackTrace/Log dili karışık; kullanıcı görmeden sync başarısızlığı anlaşılmıyor. Küçük structured logger + SyncStatus/Pending/FailedLastAttempt; kişisel/veri/token içeriği loglanmaz. Crash reporting (örn. Crashlytics) opsiyonel işletim aracı, mimari önkoşul değil. CI/format/lint/detekt tek ölçülü quality gate düşünülebilir; mevcut projede pipeline görünmüyor ve bu tur hiçbir analiz task'ı çalıştırılmadı.

IDE cihaz/personal XML dosyaları ekip standardı değil. Shared config ile local workspace/deployment seçimini ayır; kullanıcının mevcut staged .idea değişikliğini bu audit kapsamında kaldırma. README “tam offline”, “otomatik conflict”, “recurring reminders”, “Room relationships” ve “WorkManager genel sync” iddiaları mevcut istemcide tam karşılığı olmayan beklentiler; desteklenen davranışa göre düzelt.

## 12. AI feature'e özgü ek bulgular

### R52 — P1/P2: AI çağrısı history sync'e bağlı; hata normal mesaj gibi kaydediliyor

AiRepositoryImpl41 Firestore sync await tamamlanmadan generateContent'e geçmiyor; Firebase ağı gecikince model çağrısı da bekliyor. catch generic exception mesajını AI cevabı gibi DB'ye yazıyor. AiViewModel send isLoading kontrolü olmadan paralel gönderim; ilk tamamlanan çağrı diğerleri sürerken loading=false yapar.

Öneri: kullanıcı mesajını stable ID/status ile local kaydet, sync'i kalıcı kuyruğa bırak; model request'ini ayrı yönet. send guard veya açık queue, try/finally, retryable failed message ve request→reply bağlantısı. AI internet gerektirir; “uygulama tamamen offline” ifadesi bu özelliği kapsamaz. Kullanıcının soru/geçmişinin dış AI servisine gönderilmesi açık privacy/consent ve veri minimizasyonu konusu.

### R53 — P2: rapor boyutu, system prompt ve message timestamp

Tüm transaction geçmişi/note tek prompt'a gidiyor; boyut/kota sınırı yok, sistem talimatı ile kullanıcı verisi tek string'te. Geçmiş sorular konuşma bağlamı olarak modele gönderilmiyor; UI chat görünse de çağrı bağımsız soru. Ürün gerçekten sohbet hafızası istiyorsa bounded history/context politikası.

Öneri: aynı finans summary'den ilgili dönem/top categories/limitli satır, istemeden tüm özel notları taşıma. System instruction ayrı; not/user input güvenilmeyen data olarak yapılandırılır. Transaction olmadan budget-only kullanıcı için bilgi atlama davranışı düzeltilmeli. Model adı değiştirme configuration'ı yalnız operasyon ihtiyacı varsa Remote Config olabilir.

Sync helper timestamp alanını now ile override ediyor; AI message createdAt aynı alan olduğundan yeniden sync sırası geçmişin zamanını değiştirebilir. createdAt/updatedAt/sync version alanları ayrılmalı. Chat history pagination/retention kullanıcı beklentisiyle seçilir; sessizce eski mesajları silme yok.

## 13. Gereksiz değişiklik yapmayacağımız noktalar

- MVVM/Hilt/Room/Compose/Material3 altyapısını değiştirmiyoruz; repository pattern temelini koruyoruz.
- Single module proje yanlış değildir; feature-first paketleme hemen multi-module gerektirmez.
- Component parametre sayısı, lambda veya by/.value seçimi kendi başına bug değildir.
- WhileSubscribed5000 ve collectAsStateWithLifecycle zaten doğru kullanılan yerlerde korunur.
- Room suspend/Flow'a otomatik withContext(IO)/flowOn zinciri eklemeyiz.
- Her repository method için bir use case, her helper için interface, BaseViewModel/BaseRepository/BaseScreen veya generic Result/error framework zorunlu değil.
- Birbirine görsel olarak benzeyen fakat farklı işi olan kartları tek dev component'e zorlamayız.
- FilledBudget/AiChat mevcut küçük fonksiyonlarını korur; dosya ayırma gerekiyorsa sahiplik ve okunabilirlik için yapılır.
- CameraX, Retrofit, DataStore, Paging, modüler build ve Navigation3 gibi ayrı altyapıları mevcut gerçek ihtiyaç olmadan eklemeyiz. Mevcut Navigation Compose içinde type-safe route geçişi kullanıcının istediği kapsam dahilindedir.
- İstemci check ile server güvenliğini veya client Mutex ile çok cihazlı atomikliği çözmüş saymayız.
- Mevcut persisted field/enum/ID/link değerlerini isimlendirme temizliği sırasında kırmayız.
- Sunucu kuralları ve runtime durumuna erişmeden açıklanamayan hata için kesin kök neden iddiası yok.

## 14. Refactor sırası — üretim değişikliği öncesi küçük fazlar

1. Veri/ürün kararlarını netleştir: çıkışta pending data, para birimi, plan expiration, reminder timing, Google mevcut-account politikası. DB13 ve persisted remote/link sözleşmesini kaydet. Test yazımı ertelenmiş olsa da migration planı ertelenemez.
2. Oturum ve account isolation: session source, verified readiness, restored login, serialized logout, UID job/work/media sahipliği.
3. Veri bütünlüğü: mapper localID, insert sonucu, Result/cancellation, DB transaction/unique invariant, migrations ve money conversion.
4. Kalıcı offline akış: outbox/delete/version/reconcile/SyncStatus; immutable kayıt commands ve repository ownership.
5. Scheduled/reminder: ortak idempotent completion, unique scheduler, permission, expiration politikası, receiver/FCM/server contract.
6. Medya/location: main-safe/cancellable erişim, upload sequencing, atomic replacement, orphan cleanup, Activity launcher sınırı.
7. Feature-first taşıma ve domain/data/presentation: adlandırmalar/SDK mappers/DI; mevcut Navigation Compose içinde type-safe route ve graph/back-stack/deep-link geçişi; her fazda küçük dosya planı, public/serialized compatibility korunarak.
8. UI state ve decomposition: auth/add/detail/history/home/AI/budget/map tek tek; route/content, guards, lifecycle, saved state ve hata/metin/tema/a11y tutarlılığı.
9. SDK/build/release: Credential Manager, AI Logic/App Check/proxy seçimi; catalog/BOM/unused/direct dependencies, Manifest/backup/deep links/README.
10. Kullanıcının istediği gibi bütün refactor bittikten sonra ayrı test/derleme/lint/cihaz/kabul aşaması. Bu raporda o aşamadan hiçbir sonuç yok.

Tek fazda tüm proje dosyalarını taşımak ve para/schema/SDK/UI değişimini karıştırmak review'ı zorlaştırır. Hedef her fazda sınırlı sahiplik, anlaşılır diff ve yeni durumda tekrar uygulanabilir kontrol listesi.

## 15. Uygulamadan önce kullanıcı kararı gereken davranışlar

- Logout: upload bekleyen offline finansal veri aynı hesaba ait yerel alanda korunsun mu; “bu cihazdaki veriyi kaldır” ayrı eylem mi? Öneri: kayıp olmadan account-scoped saklama, kesin hesap izolasyonu.
- Para: yalnız TRY mi, gerçek çoklu currency mi? Öneri: mevcut Türkçe/TL UX için açık TRY ve minor unit; yeni FX özelliği ekleme.
- Expired scheduled kayıt: korunmuş Expired mi, gerçekten otomatik silinsin mi? Öneri: bildirim bitti diye finansal niyeti otomatik silmeme.
- Reminder: seçilen gün hangi saat/sıklık, dismiss ile snooze aynı şey mi, tam zaman garantisi gerçekten gerekli mi?
- Google: mevcut kullanıcı giriş politikası korunacak mı; Google ile yeni kayıt/provider linking nasıl olmalı?
- Password reset: ad/soyad sorgusu korunacak mı; öneri native email reset ve hesap varlığını ifşa etmeyen sonuç.
- Tema: koyu tasarım mı, gerçek light/dynamic destek mi?
- AI: Cloud Firebase AI Logic/App Check mi, authenticated mevcut server/Functions proxy mi? Geçmiş/özel notların gönderilme kapsamı?

Bu kararlar için bu turda davranış değiştirilmedi; bunlar refactor ile karıştırılmaması gereken ürün/sunucu sınırları.

## 16. Dosya kapsam matrisi

Aşağıdaki üretim kaynaklarının her biri okundu. “Koru” sıfır değişiklik garantisi değil; her dosyada sorun icat edilmediğini gösterir. Gerekli paket/import/model uyarlaması fazında komşu dosyaları da etkileyebilir. Kod satırı bağlantıları mevcut snapshot'a aittir.

| # | Okunan üretim dosyası | Sonuç / ilgili bulgu |
|---|---|---|
| 1 | [AndroidManifest.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/AndroidManifest.xml) | R23, R45, R47–48, R55–56 — izin/initializer/link/key/backup. |
| 2 | [MainActivity.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/MainActivity.kt) | R07, R45, R47 — session/startup ve cold/warm intent sınırı. |
| 3 | [ai_repository/AiRepository.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/ai_repository/AiRepository.kt) | R32, R52 — domain sözleşmesi, SDK/Room bağımlılığı. |
| 4 | [ai_repository/AiRepositoryImpl.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/ai_repository/AiRepositoryImpl.kt) | R13–16, R33, R40, R52–53 — finans summary, AI çağrısı, hata/context. |
| 5 | [components/EditAlertDialog.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/components/EditAlertDialog.kt) | R35, R41–43 — isim ve ortak dialog API'sini ölçülü koru. |
| 6 | [components/EditButton.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/components/EditButton.kt) | R35, R42, R57 — ortak eylem görünümü/tema. |
| 7 | [components/EditTextField.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/components/EditTextField.kt) | R35, R39, R42–43 — API sayısı tek başına kusur değil; field semantics/tema. |
| 8 | [components/EditTopBar.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/components/EditTopBar.kt) | R35, R43, R46, R54 — StringRes, back/a11y ve route sahipliği. |
| 9 | [di/application/FinanceApplication.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/di/application/FinanceApplication.kt) | R07, R47 — application konumu, initializer ve session startup. |
| 10 | [di/module/AiModule.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/di/module/AiModule.kt) | R32, R48–50, R55 — SDK konfigurasyonu ve boş key. |
| 11 | [di/module/DispatchersModule.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/di/module/DispatchersModule.kt) | R40 — DI dispatcher yaklaşımını koru; gerçek ihtiyaçta Default. |
| 12 | [di/module/FirebaseModule.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/di/module/FirebaseModule.kt) | R32, R55 — SDK sahipliği; singleton sağlayıcı temelini koru. |
| 13 | [di/module/RoomModule.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/di/module/RoomModule.kt) | R12 — destructive fallback yerine sürümlü migration. |
| 14 | [di/module/WorkModule.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/di/module/WorkModule.kt) | R47 — HiltWorkerFactory/WorkManager sağlayıcılarını koru. |
| 15 | [fcm/FCMNotificationSender.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/fcm/FCMNotificationSender.kt) | R22, R25, R35 — gerçek reminder schedule data-source sorumluluğu. |
| 16 | [fcm/FCMTokenManager.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/fcm/FCMTokenManager.kt) | R04, R24 — token account/lifecycle/retry. |
| 17 | [fcm/MyFirebaseMessagingService.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/fcm/MyFirebaseMessagingService.kt) | R23–24 — token callback, UID doğrulama ve servis job. |
| 18 | [feature/AuthNavGraph.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/feature/AuthNavGraph.kt) | R38, R45–46, R54 — auth graph/typed routes/reset args. |
| 19 | [feature/HandleDeepLinks.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/feature/HandleDeepLinks.kt) | R45, R54 — tek parser, session gate ve tek tüketim. |
| 20 | [feature/MainNavConstants.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/feature/MainNavConstants.kt) | R35, R46, R54 — yinelenen route kaynaklarını kaldır. |
| 21 | [feature/MainNavGraph.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/feature/MainNavGraph.kt) | R38, R46, R54 — typed graph ve navigation boundary. |
| 22 | [firebasemodel/User.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebasemodel/User.kt) | R09, R32 — remote profile DTO/domain sahipliği. |
| 23 | [firebaserepo/AuthRepository.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebaserepo/AuthRepository.kt) | R32 — UI/SDK'dan bağımsız auth sözleşmesi. |
| 24 | [firebaserepo/AuthRepositoryImpl.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebaserepo/AuthRepositoryImpl.kt) | R04–09, R40, R50 — serialized session, auth/provider ve typed failure. |
| 25 | [firebasesync/FirebaseSyncService.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebasesync/FirebaseSyncService.kt) | R01–06, R10–11, R18, R22, R29, R33, R40, R53 — temel ayrıştırma/veri bütünlüğü. |
| 26 | [firebasesync/SyncType.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebasesync/SyncType.kt) | R10, R33 — persisted sync türü/işlem sahipliği; seri değer uyumunu koru. |
| 27 | [location/LocationData.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/location/LocationData.kt) | R31–32 — immutable seçili konum değeri; layer sahipliği. |
| 28 | [location/LocationPickerUiState.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/location/LocationPickerUiState.kt) | R31, R36 — tek state ve açık permission/loading/error. |
| 29 | [location/LocationPickerViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/location/LocationPickerViewModel.kt) | R31, R40 — cancellable location/geocode; late result. |
| 30 | [location/LocationUtil.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/location/LocationUtil.kt) | R31, R40 — main-safe SDK adapter. |
| 31 | [location/MapLocationPickerScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/location/MapLocationPickerScreen.kt) | R31, R38, R43 — marker/input state ve izin/error UX. |
| 32 | [navigation/FinanceNavigation.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/navigation/FinanceNavigation.kt) | R35, R38, R46, R54 — package/graph/root session sahipliği. |
| 33 | [navigation/NavExtension.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/navigation/NavExtension.kt) | R46, R54 — popUpTo/saveState/restoreState ve typed graph. |
| 34 | [navigation/Screens.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/navigation/Screens.kt) | R35, R46, R54 — string enum yerine typed route model. |
| 35 | [navigation/bottomnavigation/BottomNavItem.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/navigation/bottomnavigation/BottomNavItem.kt) | R35, R46, R54 — typed destination identity; indeksle rol belirleme yok. |
| 36 | [navigation/bottomnavigation/BottomNavigation.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/navigation/bottomnavigation/BottomNavigation.kt) | R43, R46, R54 — hasRoute/tab state/back-stack. |
| 37 | [notification/DeleteExpiredNotification.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/notification/DeleteExpiredNotification.kt) | R20, R35 — finans kaydını silme politikası ve gerçek isim. |
| 38 | [notification/NotificationActionReceiver.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/notification/NotificationActionReceiver.kt) | R02, R19, R21–25 — atomic completion, receiver ömrü/unique work. |
| 39 | [notification/NotificationWorker.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/notification/NotificationWorker.kt) | R18, R22–23 — due-time/permission/account guard. |
| 40 | [photo/PhotoHelper.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/photo/PhotoHelper.kt) | R30, R40 — Activity launcher/permission/temp cleanup. |
| 41 | [photo/PhotoMoveWorker.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/photo/PhotoMoveWorker.kt) | R19, R26–29, R40 — owner ve işlem sırası/idempotence. |
| 42 | [photo/PhotoSourceBottomSheet.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/photo/PhotoSourceBottomSheet.kt) | R30, R43, R57 — mevcut seçim UI'sını koru; token/a11y. |
| 43 | [photo/PhotoStorageManager.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/photo/PhotoStorageManager.kt) | R26–29, R40 — upload/download domain sonucu/account sahipliği. |
| 44 | [photo/PhotoStorageUtil.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/photo/PhotoStorageUtil.kt) | R27–29, R40 — stream/decode/IO/EXIF/temp yaşam süresi. |
| 45 | [photo/PhotoUploadWorker.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/photo/PhotoUploadWorker.kt) | R26–29, R40 — belge hazır değilken success; upload state. |
| 46 | [roomdb/converters/Converters.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/converters/Converters.kt) | R12, R35 — enum/storage compatibility; gereksiz değişim yok. |
| 47 | [roomdb/dao/AiMessageDao.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/dao/AiMessageDao.kt) | R05, R10–12, R53 — account/ID/status ve createdAt. |
| 48 | [roomdb/dao/BudgetDao.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/dao/BudgetDao.kt) | R05, R12, R15, R35 — unique invariant/query isimleri. |
| 49 | [roomdb/dao/ScheduledTransactionDao.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/dao/ScheduledTransactionDao.kt) | R05, R12, R19–20 — atomik completion/account sorgusu. |
| 50 | [roomdb/dao/TransactionDao.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/dao/TransactionDao.kt) | R05, R12–16 — account/money/date sorguları ve stable IDs. |
| 51 | [roomdb/database/FinanceDatabase.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/database/FinanceDatabase.kt) | R05, R12 — version/schema/migration ve transaction sınırı. |
| 52 | [roomdb/entitiy/AiMessageEntity.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/entitiy/AiMessageEntity.kt) | R05, R10–12, R53 — owner/status/createdAt, remote kimlik. |
| 53 | [roomdb/entitiy/BudgetEntity.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/entitiy/BudgetEntity.kt) | R05, R12–15 — owner/para/limit invariants. |
| 54 | [roomdb/entitiy/ScheduledTransactionEntity.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/entitiy/ScheduledTransactionEntity.kt) | R05, R12–14, R18–20 — owner/due/status/idempotence. |
| 55 | [roomdb/entitiy/TransactionEntity.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/entitiy/TransactionEntity.kt) | R05, R12–16 — owner/para/tarih/remote kimlik. |
| 56 | [roomdb/type/Type.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomdb/type/Type.kt) | R13, R35 — persisted enum isimleri korunarak domain sahipliği. |
| 57 | [roommodel/CategoryExpense.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roommodel/CategoryExpense.kt) | R15, R32 — query projection/domain summary ayrımı. |
| 58 | [roomrepository/budgetrepositroy/BudgetRepository.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomrepository/budgetrepositroy/BudgetRepository.kt) | R32 — UI yerine domain sözleşmesi. |
| 59 | [roomrepository/budgetrepositroy/BudgetRepositoryImpl.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomrepository/budgetrepositroy/BudgetRepositoryImpl.kt) | R12, R14–15, R32, R40 — invariant/main-safe repository. |
| 60 | [roomrepository/financerepository/FinanceRepository.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomrepository/financerepository/FinanceRepository.kt) | R19, R32 — domain sonuç/atomik commands sözleşmesi. |
| 61 | [roomrepository/financerepository/FinanceRepositoryImpl.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/roomrepository/financerepository/FinanceRepositoryImpl.kt) | R12–16, R19, R32, R40 — local işlemler ve SDK'dan bağımsız sınır. |
| 62 | [screens/auth/AuthExceptions.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/auth/AuthExceptions.kt) | R32, R41 — data→presentation bağımlılığını kaldır; typed auth failures. |
| 63 | [screens/auth/AuthState.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/auth/AuthState.kt) | R36, R38 — işlem bazlı immutable auth state. |
| 64 | [screens/auth/AuthViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/auth/AuthViewModel.kt) | R08–09, R36, R38, R40, R50 — provider/verification/state. |
| 65 | [screens/auth/PasswordResetRequestScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/auth/PasswordResetRequestScreen.kt) | R38, R41–43, R57 — ortak auth form/sabit yerleşim. |
| 66 | [screens/auth/PasswordResetScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/auth/PasswordResetScreen.kt) | R38, R41–43, R45, R57 — reset link/state/form ve responsive yerleşim. |
| 67 | [screens/auth/SignInScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/auth/SignInScreen.kt) | R30, R38, R43–44, R50, R57 — auth shell/provider/BackHandler. |
| 68 | [screens/auth/SignUpScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/auth/SignUpScreen.kt) | R38, R41–43, R57 — auth shell/field validation. |
| 69 | [screens/main/addtransaction/AddTransactionScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/addtransaction/AddTransactionScreen.kt) | R30, R36, R38–39, R41–43, R57 — route/content/form/photo/location decomposition. |
| 70 | [screens/main/addtransaction/AddTransactionViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/addtransaction/AddTransactionViewModel.kt) | R13–14, R18, R26–30, R36, R40 — doğrulama/scheduling/media/save guard. |
| 71 | [screens/main/addtransaction/DataPickerField.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/addtransaction/DataPickerField.kt) | R16, R35, R42, R57 — DatePicker adı, UTC tarih/tema. |
| 72 | [screens/main/addtransaction/ReminderSwitch.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/addtransaction/ReminderSwitch.kt) | R18, R35, R42, R57 — reminder state ve shared field yüzeyi. |
| 73 | [screens/main/aichat/AiChatScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/aichat/AiChatScreen.kt) | R38, R43–44, R52 — stable keys/scroll/failed-message UI. |
| 74 | [screens/main/aichat/AiViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/aichat/AiViewModel.kt) | R36, R40, R44, R52 — request guard/global prompt kaldırma. |
| 75 | [screens/main/budget/AddBudgetBottomSheet.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/AddBudgetBottomSheet.kt) | R14, R36, R41–43, R57 — validasyon ve gerçek ortak alanlar/token. |
| 76 | [screens/main/budget/AddLimitButton.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/AddLimitButton.kt) | R35, R42–43, R57 — gerçek eylem adı ve button token. |
| 77 | [screens/main/budget/BudgetEvent.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/BudgetEvent.kt) | R38, R41 — transient event/state ayrımı ve typed args. |
| 78 | [screens/main/budget/BudgetScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/BudgetScreen.kt) | R35–38, R41–43 — route/content ve tema/error/dialog. |
| 79 | [screens/main/budget/BudgetState.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/BudgetState.kt) | R36–37 — üç anlamlı state yüzeyini gerekçe olmadan tek state'e zorlama. |
| 80 | [screens/main/budget/BudgetViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/BudgetViewModel.kt) | R02, R12–16, R36, R40 — insertID, money/percent ve immutable draft. |
| 81 | [screens/main/budget/EmptyBudgetContent.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/EmptyBudgetContent.kt) | R39, R42–43, R57 — mevcut küçük content fonksiyonları/tema. |
| 82 | [screens/main/budget/FilledBudgetContent.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/FilledBudgetContent.kt) | R14–15, R35, R39, R42–43, R57 — money/limits ve semantic renk. |
| 83 | [screens/main/budget/SmoothLinearProgress.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/SmoothLinearProgress.kt) | R17, R43 — doğru ratio/progress semantics. |
| 84 | [screens/main/history/DetailScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/history/DetailScreen.kt) | R30, R36, R39, R41–43, R57 — route/content/editor/launcher ayrımı. |
| 85 | [screens/main/history/DetailViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/history/DetailViewModel.kt) | R03, R10, R14, R27, R36, R40, R46, R54 — save/sync/delete ve typed ID. |
| 86 | [screens/main/history/TransactionHistoryScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/history/TransactionHistoryScreen.kt) | R35–37, R42–44, R46, R54, R57 — filters/stable keys/card route. |
| 87 | [screens/main/history/TransactionHistoryViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/history/TransactionHistoryViewModel.kt) | R16, R36–37 — filter state/flatMapLatest ve dönem. |
| 88 | [screens/main/home/AiSuggestionState.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/home/AiSuggestionState.kt) | R15, R41, R44 — typed suggestion/UI resource ve prompt intent. |
| 89 | [screens/main/home/HomeScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/home/HomeScreen.kt) | R17, R42–44, R57 — shared gradient/layout/modifier ve a11y. |
| 90 | [screens/main/home/HomeUiState.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/home/HomeUiState.kt) | R17, R36–37 — doğru isimli summary/loading state. |
| 91 | [screens/main/home/HomeViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/home/HomeViewModel.kt) | R13, R15–17, R36–37, R41, R57 — summary, dönem ve kod içi metinler. |
| 92 | [screens/main/schedule/ScheduleViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/schedule/ScheduleViewModel.kt) | R10, R18–22, R36, R40 — ortak idempotent scheduled command. |
| 93 | [screens/main/schedule/ScheduledTransactionScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/schedule/ScheduledTransactionScreen.kt) | R19, R38, R43 — stable key korunur; action state/route. |
| 94 | [screens/splash/SplashScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/splash/SplashScreen.kt) | R07–08, R44, R50 — delay yerine session readiness/startup. |
| 95 | [ui/theme/Color.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/ui/theme/Color.kt) | R42, R57 — palette ve semantic ek renk/gradient. |
| 96 | [ui/theme/TextFieldStyles.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/ui/theme/TextFieldStyles.kt) | R42, R57 — ColorScheme yüzeyi; hardcoded rengi merkezileştir. |
| 97 | [ui/theme/Theme.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/ui/theme/Theme.kt) | R42, R57 — dark/light/dynamic kararı; tek Compose tema kaynağı. |
| 98 | [ui/theme/Type.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/ui/theme/Type.kt) | R42–43, R57 — typography temelini koru/font scale. |
| 99 | [utils/CategoryTypeToIconResId.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/utils/CategoryTypeToIconResId.kt) | R32, R35 — presentation mapper; sabit ikon mapping korunur. |
| 100 | [utils/CategoryTypeToResId.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/utils/CategoryTypeToResId.kt) | R32, R41 — presentation localization mapper. |
| 101 | [utils/DateFormatter.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/utils/DateFormatter.kt) | R15–16, R32 — Clock/java.time ve label→business-range bağımlılığını kaldır. |
| 102 | [utils/ExpensePieChart.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/utils/ExpensePieChart.kt) | R42–44, R57 — chart palette/px text/semantics/allocation. |
| 103 | [utils/FinanceDropdownMenu.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/utils/FinanceDropdownMenu.kt) | R35, R43 — ortak UI sahipliği/semantics; ölçülü koru. |
| 104 | [utils/FormatExtensions.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/utils/FormatExtensions.kt) | R13–14, R32 — explicit currency/locale-safe money formatting. |
| 105 | [res/drawable/ic_launcher_background.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/drawable/ic_launcher_background.xml) | Koru — launcher vector/brand çizimi; UI tema rengi ile körlemesine birleştirme. |
| 106 | [res/drawable/ic_launcher_foreground.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/drawable/ic_launcher_foreground.xml) | Koru — launcher vector/adaptive icon uyumu. |
| 107 | [res/drawable/ic_notification.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/drawable/ic_notification.xml) | Koru — notification small icon kaynağı; mevcut staged değişiklik korunur. |
| 108 | [res/mipmap-anydpi-v26/ic_launcher.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml) | Koru — adaptive icon resource referansları. |
| 109 | [res/mipmap-anydpi-v26/ic_launcher_round.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml) | Koru — round adaptive icon resource referansları. |
| 110 | [res/values/colors.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/values/colors.xml) | R42, R57 — Android window/native resources ile Compose rol ayrımı. |
| 111 | [res/values/ic_launcher_background.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/values/ic_launcher_background.xml) | Koru — launcher resource sahipliği. |
| 112 | [res/values/strings.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/values/strings.xml) | R35, R41, R57 — mesaj tutarlılığı/duplicate/hardcoded UI kaynakları. |
| 113 | [res/values/themes.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/values/themes.xml) | R42, R47, R57 — native window background/startup ve Compose uyumu. |
| 114 | [res/xml/backup_rules.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/xml/backup_rules.xml) | R48 — finans/pending/account backup politikası. |
| 115 | [res/xml/data_extraction_rules.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/xml/data_extraction_rules.xml) | R48 — cihaz transferi/cloud backup sınırı. |
| 116 | [res/xml/file_paths.xml](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/res/xml/file_paths.xml) | R30, R47 — dar FileProvider path korunur; temp medya yaşam süresi. |

## 17. Son güncellik kontrolü: type-safe navigation ve ölçülü API geçişleri

### R54 — P2: Navigation Compose type-safe route geçişi istenen kapsamda

Mevcut dependency Navigation Compose 2.9.4; type-safe Kotlin route API'leri için yeterli sürüm ailesi. Navigation3, XML Safe Args veya yeni navigation framework gerekli değil. Kullanıcının istediği geçiş refactor planına dahil edildi; bu tur uygulanmadı. Bu bir yalnız stil değişimi değil: dağınık string destination/argument üretimini ve argüman hatalarını azaltan sözleşme düzeltmesi. [Resmî type-safe navigation rehberi](https://developer.android.com/guide/navigation/design/type-safety).

Somut kapsam:

- Kotlin sürümüyle hizalı Kotlin Serialization plugin + uygun runtime bağımlılığı version catalog'dan yönetilir.
- Argümansız destination'lar serializable object, işlem detay destination'ı örneğin `TransactionDetail(transactionId: Int)` gibi serializable data class olur. Mevcut Int ID bu geçişte keyfi olarak Long/String'e çevrilmez.
- `composable<...>`, route instance ile `navigate(...)`, entry/SavedStateHandle üzerinden `toRoute<...>()`, selection için `hasRoute<...>()` yaklaşımı kullanılır. Graph ve popUpTo sınırları da typed yapılır.
- Room entity, ViewModel, Context, büyük finans listesi veya access token route argümanı olmaz. Minimum kimlik gönderilir; detay ilgili repository'den okunur.
- Mevcut DetailViewModel Int okuması doğru; `"transactionId"` magic string ve sessiz `?: 0` fallback kaldırılır. Typed ID olsa bile silinmiş/bulunamayan kayıt için NotFound gerekir.
- TransactionCard NavController/string route bilmez; `onTransactionClick(id)` callback'i route sahibine gider.
- External reset/schedule URI sözleşmesi korunur. URI parser doğrulaması → session gate → typed destination dönüşümü yapılır. Type safety tek başına link güvenliği, authorization veya oobCode doğrulaması sağlamaz.
- Auth sınırında eski graph temizlenir; tab state için saveState/restoreState ve start destination politikası tanımlanır. Type-safe geçiş tek başına mevcut popUpTo(0) davranışını düzeltmez.

Dosya planı: [Screens.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/navigation/Screens.kt), [FinanceNavigation.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/navigation/FinanceNavigation.kt), [NavExtension.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/navigation/NavExtension.kt), feature altındaki AuthNavGraph/MainNavGraph/MainNavConstants/HandleDeepLinks, bottomnavigation iki dosyası, EditTopBar, TransactionHistoryScreen içindeki TransactionCard, DetailViewModel ve Gradle/catalog. Bu dosyalar aynı navigation fazında küçük kontrollü alt adımlarla ele alınır. Root/feature route sahipliği R34'e göre düzenlenir; bütün route'ları Screen enum'u yanında ikinci bir kaynakta tutmayız.

Diğer güncellik kararları:

| Alan | Şimdiki durum | Ölçülü karar |
|---|---|---|
| Google giriş | Legacy GoogleSignIn kullanılıyor; Credential Manager/googleid dependency mevcut | Deprecated akışı Credential Manager'a geçir; provider/linking/authorization politikasını koru. [Geçiş rehberi](https://developer.android.com/identity/sign-in/legacy-gsi-migration). |
| Gemini | Eski Google AI client SDK; config boş veya sağlanırsa APK'da | Firebase AI Logic/App Check ya da authenticated server proxy; hata/privacy/context davranışlarıyla birlikte ele al. [SDK geçişi](https://firebase.google.com/docs/ai-logic/migrate-from-google-ai-client-sdks). |
| Kotlin Gradle | kotlinOptions/jvmTarget string yaklaşımı | Typed compilerOptions'a kontrollü geçiş. Derleme hedefini sırf syntax güncellendi diye değiştirme. [Kotlin compiler options](https://kotlinlang.org/docs/gradle-compiler-options.html). |
| Hilt Compose | Yeni hiltViewModel package import'u zaten kullanılıyor; dependency hilt-navigation-compose | Uyumlu sürümde hilt-lifecycle-viewmodel-compose doğrudan artifact sahipliği değerlendirilsin. Yeni API zaten kullanılan yerde tekrar geçiş icat etme. [Hilt release notes](https://developer.android.com/jetpack/androidx/releases/hilt). |
| Fotoğraf seçimi | GetContent + TakePicture/FileProvider | GetContent geçersiz/deprecated sayılmıyor. Yalnız görüntü seçimi için PickVisualMedia isteğe bağlı sade iyileştirme; background URI erişimi/local copy korunmalı. CameraX gerekmez. [Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker). |
| Geocoder | API33 async, eski cihazlarda blocking çağrı | Eski platform desteğini kaldırmak yerine IO main-safety; callback/error/cancellation adapter. |
| Tarih | Calendar/statik now ve stale range | minSdk30 nedeniyle java.time kullanılabilir; ortak Clock/dönem hesabı. Bu ihtiyaç için ayrıca gereksiz desugaring altyapısı ekleme. |
| Splash | Compose ekranında sabit 3 saniye | Session readiness zorunlu düzeltme; sistem SplashScreen API startup UX için isteğe bağlı. [Splash migration](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate). |
| Map marker | rememberMarkerState ile ilk konuma bağlı state | Dependency'nin sunduğu uygun updated-marker API'si veya açık tek state sahibi; drag seçimini harici update ile ezme. [Resmî Maps Compose kaynağı](https://github.com/googlemaps/android-maps-compose). |
| Room | REPLACE, destructive migration, owner/unique eksikleri | Migration/transaction/unique invariant önce. Upsert yalnız gerçek anahtar sözleşmesine uygunsa; modern isim diye kör değişim yok. |
| Lifecycle | collectAsStateWithLifecycle ve WhileSubscribed çoğu yerde mevcut | Doğru kullanılan API'leri koru. DisposableEffect observer'ını sırf daha yeni API var diye taşımak öncelik değil. |
| Büyük library geçişleri | Coil/WorkManager/Room/Hilt temeli çalışmanın parçası | Navigation3/Coil3/KSP2/AGP büyük sürüm/multi-module, ihtiyaç ve compatibility kanıtı olmadan bu faza eklenmez. |

Bu tablo “en yeni dependency numaraları” listesi değildir. Yeni Hilt/AGP/Kotlin sürümlerinin tooling gereksinimleri farklı olabilir; mevcut sürüm zinciri resmi compatibility ve gerçek derleme aşamasında ayrıca doğrulanır.

## 18. Anahtarlar: APK, dosya konumu ve GitHub sınırı

### R55 — P1/P2: client API key ile gerçek secret ayrımı ve Git kanıtı

Bu bölümde hiçbir anahtar/token/parola değeri yazılmadı. Tracked dosya, ignore ve geçmiş taraması read-only yapıldı.

| Değer / dosya | Konum ve APK etkisi | Git durumu / değerlendirme |
|---|---|---|
| Firebase client config / API key | [app/google-services.json](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/google-services.json); plugin istemci resource'larını üretir, APK'da istemci tanımlayıcıları bulunabilir | Tracked; API key örüntüsü yerel origin/master snapshot'ında da var. Firebase istemci key'i service-account private key değildir. Yalnız Firebase API kısıtları, Security Rules ve App Check doğruysa client config'te bulunması normaldir. |
| Maps Android API key | [Manifest](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/AndroidManifest.xml:37) içinde literal; kaynak ve APK metadata'sından okunabilir | Tracked/geçmişte ve yerel origin/master snapshot'ında var. local/CI + manifest placeholder kaynak kontrolü hijyeni sağlar; APK'da gizli yapmaz. Android package/signing SHA ve sadece gerekli API restrictions Console'da kontrol edilmeli. |
| Gemini Developer API key | [app Gradle](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/build.gradle.kts:28) local.properties property → BuildConfigField; [AiModule](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/di/module/AiModule.kt) onu alır | Şu an local.properties içinde GEMINI_API_KEY yok ve mevcut üretilmiş debug BuildConfig boş. Bu inceleme Gemini key sızıntısı bulmadı; eklendiğinde APK'ya gömme tasarımı güvenli değil. Empty fallback kullanıcıya anlamlı config/feature durumu vermiyor. |
| Google OAuth client ID | Firebase config ve üretilmiş client resource | İstemci kimliği public yapılandırmadır; OAuth client secret/private key ile karıştırılmamalı. Package/SHA/provider config uyumu giriş için ayrıca gerekir. |
| FCM registration token | Runtime SDK değeri; token manager Firestore'a kayıt yapıyor | Kaynakta private server credential değil. Hesap bağlama, logout/revocation/retry ve log/backup sızıntısını önleme gerekir; R04/R24. |
| Service account private key / OAuth client secret | Kontrol edilen tracked dosya ve Git geçmişinde yaygın private-key/client-secret örüntüleri | Bu örüntülerle eşleşme bulunmadı. Bu, bütün uzak branch/fork/artifact/console için adli tarama garantisi değildir. |

Firebase API key yalnız tanımlayıcıdır; veriyi owner tabanlı Firestore/Storage Rules ve uygun App Check/authorization korur. Gemini için kullanılan key bunun istisnasıdır; public Firebase key'in izin listesine Gemini Developer API'yi eklemek güvenli çözüm değildir. Bu Console izinleri checkout'tan doğrulanamadı. [Firebase anahtar güvenliği](https://firebase.google.com/docs/projects/api-keys), [Maps key kısıtları](https://developers.google.com/maps/api-security-best-practices).

GitHub konusunda kesin ifade:

- Mevcut Manifest ve google-services.json Git tarafından takip ediliyor; normal commit/push ile gider. Anahtar örüntüleri yerel `refs/remotes/origin/master` `ac4e38d` snapshot'ında da bulundu ve önceki commit'lerde mevcut.
- google-services.json'daki bu tur öncesinden kalan unstaged güncellemenin tamamının GitHub'a gittiği söylenmiyor; tracked olmak, mevcut değişikliğin push edilmiş olması değildir.
- `git ls-remote --heads origin` DNS nedeniyle tamamlanmadı. Dolayısıyla canlı GitHub'ın bugünkü içeriği/visibility'si ayrıca doğrulanamadı; yerel remote-tracking kanıtı canlı sunucu sorgusu gibi sunulmadı.
- local.properties .gitignore kapsamında ve tracked değil; kontrol edilen erişilebilir Git geçmişinde bu dosya kaydı bulunmadı. Şu an yalnız sdk.dir var.
- .gitignore'a satır eklemek önceden committed key'i geçmişten çıkarmaz. Gerçek private key sızıntısı saptanırsa önce revoke/rotate ve erişim/kota kontrolü, gerekirse ekipçe history cleanup gerekir. Bu tur hiçbir key rotate/revoke/console/history mutation yapılmadı.

AI yapılandırması için öneri: seçilen Firebase AI Logic/proxy yoluna geç; yoksa boş anahtarla SDK başlatıp kullanıcıya generic hata vermek yerine AI feature'ı açıklayıcı biçimde devre dışı bırak veya gerekli build config'i açık doğrula. Anahtarı BuildConfig/local.properties'e koymak tek başına “güvenli yerde” demek değildir.

### R56 — P2: APK imzalama anahtarı ve gelecekte secret commit koruması

- Varsayılan debug keystore `/Users/ahmetkaragunlu/.android/debug.keystore` mevcut ve repository dışında. Kontrol edilen tracked dosyalarda .jks/.keystore/private key bulunmadı.
- [app/build.gradle.kts](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/build.gradle.kts) içinde custom release signingConfigs/storeFile/keyAlias/password yapılandırması yok. Bu nedenle “release anahtarı doğru ayarlanmış” diyemeyiz; başka yerde release/upload key veya Play App Signing olup olmadığı bu checkout'tan anlaşılmaz.
- Debug key dağıtım/upload güvenlik anahtarı değildir. Release/upload private key repository dışında korumalı dosya/secret store'da; parolalar local veya CI secret olarak yönetilmeli. APK'ya public signing certificate eklenir; private signing key eklenmez. Firebase'e verilen SHA fingerprint public sertifika özetidir, gizli keystore/parola değildir. [Android signing rehberi](https://developer.android.com/studio/publish/app-signing).
- Mevcut .gitignore local.properties'i koruyor; fakat *.jks, *.keystore, .env ve service-account credential dosyalarına özel koruma yok. Şu an bunlar tracked bulunmadı; ileride yanlışlıkla eklenmemeleri için dar, anlaşılır ignore kuralları ve CI secret scanning önerilir.
- PEM uzantılı her dosya secret değildir; public certificate ile private key ayırt edilmeli. google-services.json'ı sırf JSON/API key içeriyor diye otomatik silmek veya bütün JSON'ları ignore etmek doğru çözüm değil.
- Dosya ignore kuralları/private key saklama, APK API-key güvenliği ve Console API restrictions üç farklı güvenlik sınırıdır.

## 19. Ek hardcode kontrolü: metin, renk, dp ve merkezi tasarım yönetimi

### R57 — P2/P3: merkezi tema/token yönetimi gerekli ama her literal kusur değil

Evet, hardcoded kullanıcı metni, renk ve dp var. Şunlar somut örnekler:

| Tür | Kaynak örneği | Karar |
|---|---|---|
| Kullanıcıya çıkan mesaj | [HomeViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/home/HomeViewModel.kt:185) ve devamı, “Harcamaların artıyor…” gibi messageText | Suggestion türü + parametreleri UI state'e ver; XML string format'ını presentation'da seç. VM'de locale'e bağlı hazır cümle üretme. |
| UI'dan gönderilen AI prompt | [HomeViewModel.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/home/HomeViewModel.kt:186) ve devamı | Kullanıcıya görünen/soru olarak gönderilen localized prompt ile domain/system instruction ayrılmalı. UI metni strings; model davranış talimatı ait olduğu AI katmanında yönetilir. |
| Tekrarlanan field yüzeyi | [TextFieldStyles.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/ui/theme/TextFieldStyles.kt:23), [AddTransactionScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/addtransaction/AddTransactionScreen.kt:102), ReminderSwitch, Detail/History | Aynı 0xFF353b45 family çok yerde; ColorScheme surfaceContainer benzeri doğru rol veya tanımlı ortak semantic fieldSurface. |
| Tekrarlanan gradient | [HomeScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/home/HomeScreen.kt:67), [EmptyBudgetContent.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/EmptyBudgetContent.kt:47), [FilledBudgetContent.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/FilledBudgetContent.kt:102) | Aynı mor/mavi çift için tek semantic summaryGradient; auth gradient'i ayrı ürün rolü olabilir. |
| Durum rengi | [FilledBudgetContent.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/FilledBudgetContent.kt:380) — kırmızı/turuncu/yeşil | error/warning/success veya budget durum rengi; yalnız renk ile durum anlatılmasın. |
| Ortak spacing/shape | [AddBudgetBottomSheet.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/main/budget/AddBudgetBottomSheet.kt:51), [PhotoSourceBottomSheet.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/photo/PhotoSourceBottomSheet.kt:40), auth/main ekranları | 8/12/16/24dp tekrarları; gerçek ortak small/medium/large spacing, screenPadding, cardShape/sheetShape ölçüleri. |
| Yerleşim için sabit büyük değer | [PasswordResetScreen.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/screens/auth/PasswordResetScreen.kt:97), request ekranında da top 240dp / button width 280dp | Bir Dimensions constant'a taşıma yeterli değil; adaptive widthIn, insets/IME ve hizalama/scroll düzeltmesi. |
| Grafik palette ve px yazı | [ExpensePieChart.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/utils/ExpensePieChart.kt:184), draw içindeki textSize | Palette chart sahibinde veya gerçekten ortak Theme chartColors; yazı sp/density/font scale dikkate alınarak çizilir. |
| Android pencere/theme XML | colors.xml/themes.xml; Compose Theme.kt ayrı | Native başlangıç/window renkleri resource olarak kalır; Compose Material renk rolleri ana UI kaynağı. İki yüzeyin dark/light tutarlılığı sağlanır. |
| Launcher vector renkleri / teknik literal | Launcher XML, Firestore field/collection adları, log/URI anahtarları | Hepsi strings.xml/theme'ye taşınmaz. Brand çizimi, wire format ve UI localization ayrı sorumluluk. |

Önerilen küçük yapı:

- `ui/theme/Color.kt`: ürün palette. `Theme.kt`: Material3 ColorScheme, Typography, Shapes. Ana kullanım `MaterialTheme.colorScheme` / typography / shapes.
- Material ColorScheme'e gerçekten sığmayan birkaç ürün rolü için immutable `FinanceColors` veya `FinanceGradients`; tema üzerinden sağlanan tek kaynak. Bütün Material renklerini ikinci bir class'ta kopyalamayız.
- `Spacing.kt` veya `Dimensions.kt`: az sayıda gerçek ortak ölçü; örneğin screen padding, section gap, icon/action size, içerik maksimum genişliği. İhtiyaç büyümeden responsive iki ayrı token sistemi/“design system engine” kurmayız.
- Shapes için mevcut MaterialTheme.shapes'i kullan; ekranların her RoundedCornerShape'ına ayrı global isim açma. Sadece ayrı ortak card/sheet rolü varsa ek token.
- Sabit shared token'larda küçük object yeterli olabilir; tema/ekran boyutuna göre değişmesi gereken değerler gerçekten varsa CompositionLocal ile theme erişimi. CompositionLocal zorunlu değildir.
- 0.dp, tek seferlik 3.dp hizalama veya yalnız bir bileşenin çizim oranı yerel kalabilir. Her 8.dp'nin aynı ürün rolü olduğunu varsayarak tek constant'a bağlamak gelecekte gereksiz coupling yaratır.
- Merkezi değeri değiştirmek yanlış hesap/kontrast/layout işini çözmez; a11y touch target ve IME responsive davranış R43 kapsamında ayrıca düzeltilir.

Ekranlarda doğrudan Color(hex) yerine semantic tema rolleri kullanmak merkezi renk yönetimini gerçek kılar. Sadece bütün hex'leri Color.kt'ye taşıyıp eski yanlış onPrimary kullanımını korumak yeterli değildir. Compose'un tema genişletme mekanizması bu küçük yapı için yeterli; ayrıca üçüncü parti design-system kütüphanesi gerekmiyor. [Resmî Compose tema genişletme rehberi](https://developer.android.com/develop/ui/compose/designsystems/custom).

## 20. Ek coroutine kontrolü: merkezi scope ve SupervisorJob kararı

### R58 — P1/P2: dört bağımsız SupervisorJob scope var; hepsini tek scope'a toplamayacağız

Doğrudan `CoroutineScope(SupervisorJob() + Dispatchers.IO)` (öğe sırası farklı olabilir) dört yerde bulundu:

| Kaynak | Mevcut sorun | Önerilen sahiplik |
|---|---|---|
| [AuthRepositoryImpl.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebaserepo/AuthRepositoryImpl.kt:34) | Logout clearAllTables bağımsız launch; signOut tamamlanma sözleşmesinin dışında | SessionCoordinator suspend/serialized logout; gerekli cleanup beklenir. Account-scoped pending veri korunur. Buradaki fire-and-forget scope için merkezi ApplicationScope'a taşımak çözüm değil. |
| [FirebaseSyncService.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/firebasesync/FirebaseSyncService.kt:57) | Singleton scope; reset listener'ları kaldırıyor ama jobs sürüyor | UID/generation sahibi session job; logout cancel + gerekli tamamlanmayı bekleme, yeniden login için yeni job. Bağımsız sync türleri hata bakımından izole edilmeli ise session SupervisorJob mantıklı. |
| [MyFirebaseMessagingService.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/fcm/MyFirebaseMessagingService.kt:37) | Service scope onDestroy'da iptal edilmiyor; ağ/DB callback işi service sonrası çalışabilir | Kısa callback → kalıcı unique WorkManager işi. Gerçekten service-owned kısa coroutine kalırsa onDestroy cancel ve injected dispatcher; process garantisi değildir. |
| [NotificationActionReceiver.kt](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/app/src/main/java/com/ahmetkaragunlu/financeai/notification/NotificationActionReceiver.kt:47) | onReceive dönüşünden sonra bağımsız scope Android'e işi yaşatmaz | Kritik eylem/durable command WorkManager + DB kaydı; kısa bounded iş için goAsync/finally finish. Receiver'a app scope vermek process death sorununu çözmez. |

`SupervisorJob`, bir child hatasının bağımsız kardeş işleri otomatik iptal etmesini önler; error handling, transaction, lock, retry veya process survival sağlamaz. Parent/session iptal edilince alt işler de iptal olmalıdır. Bir child hata verdiğinde gerektiği gibi handle/log/retry edilmesi yine gerekir; yakalanmamış launch exception'ını SupervisorJob kendiliğinden yutmaz. [Kotlin SupervisorJob sözleşmesi](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-supervisor-job.html).

Net karar:

- ViewModel'lere ayrıca yeni SupervisorJob/scope ekleme; varsayılan viewModelScope zaten SupervisorJob ve Main.immediate temelini kullanır ve ViewModel ömründe kapanır. Onu kendi global scope'umuzla değiştirmiyoruz. [AndroidX scope kaynağı](https://github.com/androidx/androidx/blob/androidx-main/lifecycle/lifecycle-viewmodel/src/commonMain/kotlin/androidx/lifecycle/viewmodel/internal/CloseableCoroutineScope.kt).
- Oturum sync'inde bağımsız türler için session-owned SupervisorJob gerekebilir; sırf dört yerde var diye kaldırmak da yanlış. Birlikte başarı/iptal gerektiren command alt adımlarında structured coroutineScope, Room transaction ve açık işlem sözleşmesi gerekir.
- Merkezi DI dispatcher tanımları uygundur; mevcut IoDispatcher korunur, gerçek CPU işi için DefaultDispatcher eklenebilir. Dispatcher işin hangi thread/context'te çalıştığını, Job/scope ise yaşam süresi/iptal sahibini belirler; aynı şey değildir.
- Merkezi `@ApplicationScope` yalnız ekran/servis dışına taşması gereken fakat süreç yaşarken tamamlanması yeterli, somut bir application-owned iş kalırsa tek sağlayıcı olarak eklenir. Şu an dört sorunlu scope'u toplamak için genel CoroutineManager kurmak önerilmiyor.
- Merkezi singleton Job'u logout'ta cancel edip sonra aynı instance'ı yeni login'de tekrar kullanmak yanlış; cancelled job yeniden aktif olmaz. Session job yeniden yaratılır, application job hesap değişiminden ayrı tutulur.
- Dayanıklı sync/upload/reminder/logout token cleanup için gerektiği yerde DB kayıt + WorkManager; ApplicationScope process ölünce kaybolur.
- scope.launch(SupervisorJob()) ile lifecycle parent'ını farkında olmadan koparmayız. CancellationException rethrow, finally cleanup ve eski UID/generation callback reddi R40/R06'daki zorunlu parçalar.

Sonuç: **SupervisorJob'a bazı sahipliklerde ihtiyaç olabilir; bütün projeyi tek merkezi coroutine scope'a geçirmek gerekmiyor.** Merkezileştirilecek şey dispatcher sağlama ve açık scope/iptal politikasıdır; her feature'ın işi kendi doğru ömrüne bağlı kalır. [Android coroutine sahipliği/DI rehberi](https://developer.android.com/kotlin/coroutines/coroutines-best-practices).

## 21. Denetimin kapanış kaydı

- 103 Kotlin + 13 XML üretim dosyası dosya-kapsam matrisinde tek tek yer alır; ana build/config/README/IDE kaynakları da ayrıca okundu.
- Bütün talep başlıkları bölüm 2 ve ayrıntılı bulgulara bağlandı; son ekler type-safe navigation, anahtar/Git güvenliği, hardcoded metin/renk/dp ve SupervisorJob/merkezi coroutine sahipliği ayrıca tamamlandı.
- R01–R58, bulgu/iyileştirme başlıklarıdır; 58 bağımsız çalıştırılmış/kanıtlanmış bug sayısı değildir. Bazıları aynı riskin farklı bileşenleri, bazıları ürün tercihi veya bakım iyileştirmesidir.
- Statik inceleme tamamlandı. Üretim refactor'ı başlamadı; test kaynakları/test yazımı/test çalıştırması yapılmadı. Derleme/lint/device kabul sonucu yok.
- Gelecek devam noktası: çalışma kaydı + bu rapordan onaylanan ilk küçük fazın dosya planı; mevcut kullanıcı değişikliklerini ve persisted veri sözleşmesini koru.

