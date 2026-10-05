# FinanceAI — Değişim Listesi

Tarih: 4 Ekim 2026.

Bu belge denetim raporundan farklıdır: rapor bulgu ve önerileri içerir; burada kullanıcıyla konuşulup kabul edilen kararlar tutulur. Öneriler otomatik olarak onaylanmış sayılmaz.

## Altın Kurallar — Maddeler 33 ve 34

Durum: Kullanıcı 33. maddenin ilkelerini kabul etti ve değişim listesinin başına altın kural olarak yerleştirilmesini istedi. Bu kurallar bütün maddeler ve uygulama fazları için geçerlidir. Faz 1 temeli kullanıcı tarafından commit edildi. 5 Ekim 2026'da Faz 2 için açık başlatma talimatı geldi; özgün kapsam ve üç ek onay uygulandı/doğrulandı; Home takvim ayı onayı uygulandı, negatif progress çizimi kullanıcı isteğiyle korundu. Güncel uygulama durumu docs/refactor-phase-2-progress.md, önceki kaynak haritası docs/refactor-phase-1-progress.md dosyasında tutulur. Faz 2 tamamlandı; son kapsam/kanıt/kullanıcı istisnaları devam kaydında kayıtlıdır.

**Zorunlu faz başlangıcı:** Her faz öncesinde Altın Kurallar ve aşağıdaki Test Kalitesi bölümlerinin tamamı kesinlikle okunacak; ilgili madde ayrıntıları ve güncel devam kaydı da okunarak somut dosya ve test planı hazırlanacak. Önceki fazda okunmuş olması yeni faz için yeterli değildir.

**Bütün fazlarda zorunlu değerlendirme:** SOLID, bağımlılıkların yönü ve DI, test edilebilirlik, genişletilebilirlik, okunabilirlik, isimlendirme, paket yapısı, katman doğruluğu, ekranların bulunduğu paket ve katmanların konum doğruluğu, kod tekrarı, gerçek ortak yapılar ve uygun merkezi yönetim birlikte dikkate alınacak. Bunlar yalnız Faz 1 veya tek örnek fonksiyon için değil, dokunulan bütün kod için geçerlidir. Over-engineering kesinlikle yok; yalnız gerçek ihtiyaç için yapı eklenir. **Mevcut işleyiş ve tasarım asla değiştirilmeyecek.** Ayrı davranış/tasarım değişikliği gerektiren bulgu refactorun dışında tutulup somut etkisi kullanıcıya sunulacak; ayrıca onay verilmedikçe uygulanmayacak.

1. **Her değişikliğin somut gerekçesi olacak.** Okunabilirlik, sorumluluk ayrımı, bağımlılık, gerçek tekrar veya test edilebilirlik ihtiyacını çözmeyen yapı eklenmeyecek. Kullanmak için teknoloji/katman kullanılmayacak.
2. **Doğru mevcut kod korunacak.** Her sınıf yeniden yazılmayacak; her metoda use case, her yardımcıya interface, BaseViewModel/BaseRepository ağı veya gereksiz framework eklenmeyecek.
3. **Parçalama sorumluluğa göre yapılacak.** Satır/parametre sayısı tek başına kusur değildir. Ayrılan parçalar anlamlı isim ve açık sahiplik taşıyacak; sırf dosyayı küçültmek için anlaşılmaz parçalama yapılmayacak.
4. **Ortaklaştırma anlam üzerinden yapılacak.** Aynı iş kuralı tek sahipte yönetilecek; yalnız görünüşü benzer ekranlar çok seçenekli dev ortak bileşene zorlanmayacak. Gerçek feature farklılıkları korunacak.
5. **Tutarlılık bütün uygulamada sağlanacak.** Eşdeğer durumlarda isimlendirme, state, callback, hata ve coroutine yaklaşımı tutarlı olacak. Farklı sorumluluk/yaşam süresi gerektiren bilinçli farklılıklar korunacak; örnek by/value veya login referansı ile sınırlı tarama yapılmayacak.
6. **Tasarım ve mevcut işleyiş korunacak.** Görünüm, renk, tipografi, yerleşim ve kullanıcı akışları değiştirilmeden refactor yapılacak. Kod taşıma/SDK geçişi mevcut özellikleri bozmayacak. Tasarım veya davranış etkileyen düzeltmeler somut etkileriyle ayrıca kullanıcı onayına sunulacak; önceki genel madde onayı bu sınırı aşma yetkisi değildir. 5 Ekim'deki özel onay: yalnız gerçek yerel/uzak kayıt çatışmasında iki sürümü koruyup kullanıcıya seçim yaptıran diyalog eklenebilir (25.5). Bu sınırlı istisna mevcut ekranları yeniden tasarlama veya her kayıtta seçim sorma izni değildir. Son ek onay: Home “Bu ay” hesabı takvim ayına geçirilir; negatif progress çizimi değiştirilmez.
7. **Kalite değerlendirmesi kanıta dayanacak.** Profesyonellik dosya/katman sayısıyla değil anlaşılır sahiplik, doğru bağımlılıklar, bakım kolaylığı ve gerçekten yapılmış doğrulamalarla değerlendirilecek. Yapılmamış test/build/lint/runtime kontrolü başarılı sayılmayacak; evrensel şirket standardı veya sıfır hata garantisi verilmeyecek.
8. **Her fazda gerçek kullanılmayan kod temizlenecek.** Fazdaki değişikliklerle boşa çıkan ve gerçekten hiçbir kullanım/giriş noktası olmayan dosya, ekran, fonksiyon, sınıf, import, kullanılmayan XML kaynağı ve kod kaldırılacak. Boş eski paketler bırakılmayacak. Yalnız metinsel referans yokluğu silme kanıtı değildir: Manifest, Hilt/KSP, Room, reflection, navigation/deep link, persisted Worker kimlikleri ve dış sözleşmeler kontrol edilecek. Bilinçli bırakılmış uyumluluk parçaları veya sonraki fazın açık işi bu kapsama alınmaz; gerekçesi devam kaydında belirtilir. Kaynak dizininde boş klasörlerin varlığı çalışma zamanı paketi yaratmaz.
9. **Her fazda geçici konsol/debug logları temizlenecek.** Yalnız geliştirme sırasında konsolda değer/akış görmek için eklenmiş println/print ve debug logları kaldırılacak. Hata teşhisi, operasyon veya güvenli davranış takibi için gerçekten gerekli loglar korunabilir; token, anahtar, parola, kullanıcı finansal verisi, e-posta veya kişisel dosya yolu gibi hassas içerik yazılmayacak. Bütün loglar körlemesine silinmez; kalanların amacı açık olacak. Bu temizlik tasarım ve mevcut işleyişi değiştirmeyecek.

Mevcut Edit* bileşenlerini yeniden adlandırmama istisnası, uygulama öncesi dosya planı sunma ve README'yi en son hazırlama kararları geçerlidir. Test zamanlaması aşağıdaki güncel Test Kalitesi kararına tabidir.

### Test Kalitesi — Bütün fazlarda zorunlu

Durum: Kullanıcı fazlara ait testlerin yazılıp çalıştırılmasını kabul etti. Bu karar önceki yalnız refactor sonunda test inceleme/yazma/çalıştırma ertelemesinin yerine geçer. Önceki karar görüşmelerinde test yapılmadığına ilişkin tarihsel kayıtlar sonuç kanıtı olarak korunur; eski erteleme ifadeleri güncel uygulama talimatı değildir. Bu belge güncellemesi test veya üretim uygulamasını başlatmaz.

- Her fazda değişen iş kuralları, hata/iptal yolları, veri bütünlüğü ve korunması gereken davranışlar için riskle orantılı testler yazılıp çalıştırılacak; etkilenen mevcut testler incelenip gerektiğinde uyarlanacak.
- Sırf test sayısı veya coverage oranını artırmak için test yazılmayacak. Basit getter, sabit değer ve framework davranışını tekrar doğrulayan düşük değerli testlerden kaçınılacak. Bu ilke TextField veya başka tek örnekle sınırlı değildir; bütün testlere uygulanır. Önemli risk testleri gereksiz denilerek atlanmayacak.
- Testler anlamlı senaryoları ve gözlemlenebilir davranışı doğrulayacak; iç implementasyona gereksiz bağlanıp her refactorda kırılmayacak. Saf iş kuralı/VM testi yeterliyse gereksiz UI testi yazılmayacak; gerçek UI entegrasyon riski varsa uygun UI testi kullanılacak.
- Test edilen üretim sınıfının feature-first paketi aynalanacak: JVM testleri src/test, cihaz/Compose testleri src/androidTest altında aynı paket/sahiplik düzeninde olacak. Core/app sınıfları da kendi üretim paketini aynalayacak; her test zorla feature altına konmayacak.
- Gerçekten birden fazla testte kullanılan fixture/fake yardımcıları uygun test destek paketinde tutulabilir; tek feature'a zorla yerleştirilmeyecek veya gereksiz kopyalanmayacak. Yalnız bir testin kullandığı yardımcı mümkünse o testin yakınında kalacak.
- Tüm refactor sonunda kullanıcı son aşamayı istediğinde uygulamanın test kapsamı yeniden taranacak ve eksikler tamamlanacak. Faz testleri genel regresyon ve cihaz kontrollerinin yerine geçmeyecek; Faz 6 eklenmez, README en sonda kalır.
- Her faz sonunda ilgili test sonuçları açıkça raporlanacak: ne çalıştırıldı, ne geçti/başarısız oldu ve ne çalıştırılamadı belirtilecek. Çalıştırılamayan veya yalnız yazılmış test başarılı sayılmayacak; JVM ve cihaz testlerinin sonuçları ayrılacak.

Profesyonel seviye çok test değil; doğru riski yakalayan, okunabilir ve sürdürülebilir testlerdir. Test edilebilirlik gerekçesiyle üretim koduna gereksiz soyutlama veya test modunda farklı davranış eklenmeyecek. Mevcut tasarım/işleyişi koruma şartı test planında da esas alınacak.

### Madde 34 — Ürün ve davranış kararlarının sınırı

Durum: Kullanıcı aşağıdaki yedi ilkeyi de Altın Kurallar'a eklemeyi kabul etti. Bu, açık ürün seçeneklerinin sonucunu seçmek değil, karar/onay yöntemini kabul etmektir.

1. **Refactor ile davranış düzeltmesi ayrılacak.** Paket taşıma, sorumluluk parçalama, DI ve tekrar azaltma mevcut sonucu koruyacak. Kullanıcıya görünen sonucu değiştiren düzeltmenin somut etkisi ayrıca açıklanıp onay alınacak.
2. **Finansal kurallar kendiliğinden değişmeyecek.** Para birimi, yüzde tabanı, dönem anlamı ve yuvarlama mevcut kullanım üzerinden netleştirilecek; eski veriler sessizce yeni anlamla yorumlanmayacak.
3. **Bildirim ürün politikası ayrı karar olacak.** Tekrar, erteleme, missed reminder ve süresi geçen planı silme/koruma refactor bahanesiyle değişmeyecek. Teknik ayrıştırma ürün tercihini otomatik seçmez.
4. **Veri koruma tercihleri açık uygulanacak.** Hesap izolasyonu ve logout'ta pending veri koruma kabul edilmiş hedeflerdir. Sahibi belirsiz eski kayıtların migration'ı, backup kapsamı ve çok cihazlı conflict önceliği ayrıca kararlaştırılacak.
5. **AI işlevleri korunacak.** Firebase AI Logic erişim geçişi kabul edildi; model sohbet hafızası ekleme, finansal veri kapsamını değiştirme, history silme veya yeni kullanıcı limitleri otomatik eklenmeyecek. Kota/girdi sınırının mevcut akışa etkisi gerekiyorsa ayrıca açıklanacak.
6. **Tasarım etkisi ayrı onay gerektirecek.** Renk, layout, yazı boyutu, görünen hata sunumu veya grafik değişimi önce somut farkıyla sunulacak. A11y/responsive gerekçesi onaysız görsel değişiklik izni değildir.
7. **Her fazda açık karar kontrol listesi olacak.** Korunacak davranış, önerilen fark ve gereken kullanıcı onayı faz planında belirtilecek. Karar gerektirmeyen refactor ilerleyebilir; belirsiz ürün davranışı varsayımla değiştirilmez.

Temel kural: Kodun iç yapısını düzenleme onayı, ürün tasarımını veya çalışma mantığını değiştirme onayı değildir. Önceki maddeler bu sınırla uygulanacak; netleştirilmemiş tercihler tamamlanmış karar gibi raporlanmayacak.

## Beş Fazlı Uygulama Planı — Madde 35

Durum: Kullanıcı değişim listesinin beş refactor fazına ayrılmasını istedi. Aşağıdaki plan kabul edilen ayrıntıları fazlara eşler; üretim koduna başlama yetkisi değildir. Altın Kurallar en baştadır ve her faz öncesi okunur. Bütün maddelerin aşağıdaki özgün ayrıntıları korunmuştur; faz özeti onların yerine geçmez, birlikte okunur.

### Fazların ortak çalışma yöntemi

- Faz öncesi Altın Kurallar ve Test Kalitesi, ilgili özgün maddeler, checkpoint ve güncel Git durumu okunacak; dosya/sorumluluk/test planı ve açık kararlar sunulacak. Kullanıcı değişiklikleri korunacak.
- Her faz küçük anlamlı uygulama adımlarına bölünebilir; bunlar yeni faz değildir. Aynı dosyanın gereksiz tekrar taşınması önlenecek, ilgili taşıma davranış sahibinin düzenlendiği fazda yapılabilecek.
- Takip durumu kabul edildi / uygulandı / doğrulandı / karar bekliyor olarak ayrılacak. Birden fazla faza yayılan madde yalnız ilk değişiklikle tamamlandı sayılmayacak. Faz 2 tamamlandı; uygulanan adımlar, bilinçli sınırlar ve doğrulamalar docs/refactor-phase-2-progress.md içinde tutulur. Faz 3–5 henüz başlamadı.
- Tasarım, mevcut akış, veri ve dış sözleşme korunacak; açık ürün tercihi keyfi seçilmeyecek. Davranış etkileyen öneriler ayrı onay olmadan faz uygulamasına dahil edilmeyecek. İlgisiz hazır iş ilerleyebilir, karar bekleyen alt iş açık kalır.
- İlgili test kaynaklarının incelemesi, test yazımı/uyarlaması ve çalıştırılması beş fazın her birine dahildir; fazın riskleri ve Test Kalitesi kurallarıyla belirlenir. Kapsamlı son test taraması/genel regresyon/cihaz doğrulaması ve README kullanıcı ayrıca istediğinde ele alınır. Faz 6 yoktur. Build/statik kontrol otomatik test yerine geçmez; kapsam ve sonuçlar ayrı raporlanır.
- Ek Console/App Check/config adımı gerekirse kullanıcıya adım adım bildirilecek; dış sistem ayarı/ücretli plan/key rotation/Git geçmişi mutasyonu için yetki kendiliğinden varsayılmayacak.

### Faz 1 — Mimari, paketler ve bağımlılık temeli

**Ana maddeler: 1–7 ve 10.** Diğer bütün fazlar için yapısal temel; bu kalite ilkeleri sonraki fazlarda da uygulanır.

- **1 — Mimari:** MVVM/Compose/Hilt/Room/Flow/WorkManager ve tek Gradle modülü korunacak. app/core/feature sahipliği, SDK'sız domain sözleşmesi ve feature data implementasyonu kurulacak. UI'nın yerel veri kaynağı ve pending uzak işlem ayrımı için sınırlar çizilecek; dayanıklı sync uygulaması Faz 2'de tamamlanacak.
- **2 — SOLID:** Auth/sync/AI/form/planlanan işlem sorumlulukları belirlenip ayrı sahiplerine taşınacak. Gerçek genişleme sınırında küçük contract/mapper kullanılacak; her helper'a interface veya her metoda use case yok. Yerel başarı/uzak başarı/iptal sözleşmesi doğru tanımlanacak, UI türleri data/domain'e sızmayacak. Atomik iş akışı parçalama yüzünden bozulmayacak.
- **3 — Bağımlılıklar ve DI:** Repository interface domain, impl data'da olacak. VM doğrudan Firebase/DAO/WorkManager koordinasyonu yerine uygun uygulama sözleşmesini tüketecek. Constructor injection yeterliyse provider eklenmeyecek; interface binding uygun yerde Binds, SDK/factory oluşturma gerektiğinde Provides kullanılacak. Feature DI ilgili feature'da, ortak altyapı core'da, uygulama birleştirmesi gereken yerde app'te olacak; tek dev module yok.
- **4 — Test edilebilirlik:** SDK/Clock/dispatcher gibi gerçek dış sınırlar inject edilebilir ve sonuçlar gözlemlenebilir olacak. Pure hesaplama ile Android UI/launcher etkileşimi ayrılacak; fire-and-forget başarı sözleşmesi kurulmayacak. Fazın değişen sözleşme/davranışları için riskle orantılı testler ayrıca yazılıp çalıştırılacak; yalnız test için gereksiz katman kurulmayacak.
- **5 — Genişletilebilirlik:** Feature'a ait iş değişikliği ilgili feature'da kalacak, gerçek ortak kural tek sahibi üzerinden kullanılacak. Yeni adapter/iş türü için gerekenden büyük plugin/Dependencies bag/framework kurulmayacak. Persisted Worker/enum/schema uyumu korunacak.
- **6 — İsimlendirme:** Sınıf/dosya/paket ve observe/get/generate/calculate/onAction anlamları uyumlu düzenlenecek; bool/ID/plural isimleri görevini anlatacak. Kullanıcının EditTextField/EditTopBar/EditButton ve benzer Edit* ortak bileşenlerini yeniden adlandırmama istisnası korunacak. Firestore alanları, persisted değerler ve dış URI'lar kozmetik rename ile kırılmayacak.
- **7 — Paket ve ekran konumu:** feature altında domain/data/presentation gerçek sahipliğe göre yerleştirilecek; her ekran için domain/data açılmayacak. Auth signin/signup/reset, transaction add/history/detail gibi ilişkili presentation ekranları ihtiyaçlı alt paketlerde gruplanacak. DAO/entity feature data/local'da, ortak DB composition core/database'de; Route/Screen/component ve app navigation doğru katmanda olacak. Ortak UI core/ui, yalnız feature'a ait UI feature içinde kalacak; boş paket açılmayacak. FinanceApplication/MainActivity app sınırına ait olacak, di/application ekran/uygulama paketi olarak kullanılmayacak.
- **10 — Parametreler:** Fazın dokunduğu çok bağımlılıklı sınıflar sorumluluk üzerinden ayrılacak; anlamlı immutable draft/state kullanılabilecek. Parametre gizleyen dev Dependencies/Actions bag oluşturulmayacak; her Params nesnesi zorunlu değildir. Material UI bileşeninin meşru parametreleri sırf sayısı fazla diye parçalanmayacak.

**Faz sınırı:** Yapısal bağımlılık/paket temeli hazırlanır; veri migration'ı, ürün tercihi veya görünüm değişikliği sırf taşıma gerekçesiyle yapılmaz. Kalan paket taşımaları ilgili sonraki iş fazıyla birlikte tamamlanabilir.

### Faz 2 — Oturum, veri ve finansal hesaplama

**Ana maddeler: 15–17, 24–26. Madde 8'in veri/iş kuralı tekrarı bölümü.** Faz 1 sınırları kullanılır; veri sahipliği çözülmeden arka plan entegrasyonlarına geçilmez.

- **15 — Coroutine:** VM/UI/session/process/durable iş sahipliği ayrılacak; suspend/Flow sonucunun tamamlanması doğru tanımlanacak. CancellationException yeniden fırlatılacak, cleanup kontrollü olacak. Latest sorgu iptali finansal işlem iptaliyle karıştırılmayacak; Firebase Task await uzak işlemi kesin iptal eder varsayılmayacak.
- **16 — Scope/SupervisorJob:** Gerçek ihtiyaçlı process ApplicationScope Hilt qualifier ile sağlanacak; bütün işler bu scope'a taşınmayacak. Session job hesabın ömrüne bağlı ve yeniden kurulabilir olacak; logout'ta application singleton job iptal edilip yeniden kullanılamaz hale getirilmeyecek. SupervisorJob hata/transaction/hesap izolasyonu garantisi değildir. Durable işler DB + WorkManager'a ait; default viewModelScope korunacak.
- **17 — Dispatcher:** Mevcut IO sağlayıcısı korunup gerekli blocking IO sınırlarında kullanılacak; Default ağır CPU, Main provider yalnız gerçek ihtiyaçta. İş sahibi main-safe olacak; Room async/Firebase await sırf suspend olduğu için IO wrapper'a alınmayacak. Dağınık hardcoded dispatcher ve gereksiz nested context kullanımı uygun yerde düzenlenecek.
- **24 — Oturum/veri koruma:** Tek sahipli seri login/logout/startup geçişi, account-scoped kayıt/fotoğraf/pending iş ve UI state sınırı kurulacak. Eski listener/job/geç callback yeni hesabı değiştirmeyecek; kontrolsüz clearAllTables ve cancelAllWork yerine hesap kapsamı yönetilecek. Pending veriler aynı hesap için korunacak; eski sahibi belirsiz verilerin migration'ı ayrı karardır. Oturum/verification/local readiness ile offline kullanım dengesi korunacak. Bildirim/token hesap sınırının temeli bu fazda, asıl bildirim uygulaması Faz 3'te tamamlanacak.
- **25 — Room/offline sync:** Gerçek insert ID ve remote update sırasında local PK korunacak. Yerel veri + pending operation atomik kaydedilecek; network/retry/permanent failure ayrımıyla account-safe worker sync hazırlanacak. Silme niyeti uzak sonuçtan önce kaybolmayacak; hatalı işlem synced sayılmayacak. Pending'i ezmeyen reconciliation, index/unique ve veriyi koruyan schema/migration planı yapılacak. Conflict önceliği/çok cihazlı server invariant otomatik seçilmeyecek. Büyük FirebaseSyncService feature data mapper/sync sorumluluklarına ayrılacak; gerçek ortak helper korunacak.
- **26 — Para/bütçe/tarih:** Eşdeğer hesaplamalar pure ortak sahipte birleştirilecek; locale ile currency anlamı karışmayacak. TRY seçilirse Long kuruş geçişi yalnız açık karar ve Room/Firestore/form migration planıyla uygulanabilir; bütçe yüzde tabanı/dönem/yuvarlama kendiliğinden değişmeyecek. Parsing/küsurat/finite/sınır, calendar period ve Clock/java.time/date-only/dueAt tutarlılığı incelenecek. Formül/etiket/progress sonucunu değiştiren düzeltme ayrı onaya tabi olacak; yapısal çıkarım mevcut sonuçları koruyacak.
- **8 — Ortak veri iş kuralları:** Aynı scheduled completion/finans summary/sync sözleşmesinin sahipliği belirlenip gerçek tekrar kaldırılacak; generic sync bütün tür farklılıklarını gizlemeyecek. Scheduled command'in bildirim/medya uygulaması Faz 3'te, UI kullanımı Faz 5'te tamamlanacak.

**Faz sınırı:** Para temsili, conflict, migration ownership, backup ve logout politikalarının açık karar bekleyen kısımları tamamlandı sayılmaz. Kabul edilen veri koruma hedefi ile mevcut işleyiş şartı arasında görünen fark varsa uygulamadan önce somut olarak sunulur.

### Faz 3 — Arka plan işleri ve cihaz entegrasyonları

**Ana maddeler: 27–29. Madde 22'nin izin/FileProvider/Startup/WorkManager kısımları.** Maddeler 8, 15–17 ve 24–25'in bu işlere ait kalan bağlantıları tamamlanır.

- **27 — Scheduled/FCM:** Ekran ve receiver tek account-safe atomik/idempotent complete command kullanacak; pending sync/medya devri/notification cleanup sözleşmesi uyumlu olacak. Account+record unique work ve event/version dedupe kurulacak; uzun network receiver/service işi durable worker'a, kısa local iş uygun goAsync/finally finish sınırına ait olacak. Typed payload, eksik/yanlış UID reddi, geç event koruması, supplied onNewToken, observable retry ve eski token bağı yönetilecek. Sistem otomatik notification payload'ı yalnız client guard ile güvenli sayılmayacak; server sözleşmesi gerekirse ayrıca bildirilecek.
- **27 — Ürün tercihi sınırı:** dueAt, tekrar/snooze/dismiss/cancel ayrımı, missed event, expiration ve finansal plan silme davranışları açıklanıp ayrı kararla netleşecek. WorkManager exact saat garantisi sayılmaz, exact alarm otomatik eklenmez. Bildirim kaldırma finansal veri silme komutu değildir; lockscreen privacy/izin UX tasarım etkisi varsa ayrıca onay alınır.
- **28 — Fotoğraf/kamera:** TakePicture + dar FileProvider korunacak; launcher/Activity Route'ta, medya sonucu VM'de olacak. İzin/iptal/temporary cleanup tutarlılaştırılacak. Yeni fotoğraf save + DB reference commit olmadan eskisi silinmeyecek; pending upload dosyası korunacak. Local record → remote upsert → upload → ref attach sırası stable account/record/version ile bağlanacak; eksik belge erken success veya sonsuz retry olmaz. Local URI/path/remote ref ayrımı, download dedupe ve referans/pending-aware disk cleanup yapılacak.
- **28 — Ortak görüntü hazırlama:** Kamera/galeri aynı dar hazırlama yolunu kullanacak. Oranı koruyan ihtiyaçlı resize, sampled decode, EXIF, bozuk içerik kontrolü, main-safe IO/CPU ve bounded sıkıştırma uygulanacak. Küçük görsel büyütülmez, aynı dosya tekrarlı sıkıştırılmaz; fiş okunabilirliği korunur. Format/kalite/byte/piksel sınırı kullanım ihtiyacına göre netleşir; liste preview ile saklanan medya hedefi ayrı, gereksiz yeni kütüphane yok.
- **29 — Konum/harita:** Dar location/address sınırı, eski cihaz main-safe Geocoder, async success/error/cancellation ve latest selection sonucu korunacak. Coarse/fine ilk açılış/resume/permission sonucu tutarlı olacak. Seçili koordinat ve marker state tek yönlü bağlanacak; geç adres sonucu yeni seçimi ezmeyecek. Route permission/settings, feature data konum erişimi olacak; iptal kalıcı kayıtlı konumu değiştirmez. Görünür hata/marker davranış düzeltmesinin farkı gerekiyorsa ayrıca onay alınır.
- **22 — Manifest cihaz altyapısı:** Paket/sınıf referansları, exported/immutable PendingIntent, narrow FileProvider ve optional kamera korunacak. Gerçek runtime permission ihtiyacı ve merged Manifest göz önünde tutulacak. Tüm Startup provider yerine gerekli WorkManager metadata kapsamı değerlendirilip HiltWorkerFactory/Configuration.Provider bağlantısı korunacak; WorkManager'a ait boot/wake izinleri körlemesine kaldırılmayacak.

**Faz sınırı:** Ürün zamanlaması ve görünüm değiştirilmez; accepted güvenilirlik hedeflerinin davranış etkisi somutlaştırılır. Sunucu payload/snooze/çok cihazlı contract değişikliği kod refactor onayından dış yetki çıkarılarak yapılmaz.

### Faz 4 — AI ve dış servis yapılandırması

**Ana maddeler: 21, 23 ve 30. Madde 32'nin gerekli SDK/plugin/config değişiklikleri.** AI için Faz 2 finansal sözleşmeleri kullanılır.

- **21 — Ölçülü modernizasyon:** Google giriş Credential Manager geçişi Firebase/provider hesap politikası, seçim/iptal/collision/logout davranışı korunarak adapter sınırında ele alınacak. AI SDK ve typed compilerOptions gerekli uyumla güncellenecek; Geocoder/java.time Faz 2–3 bağlantılarıyla tamamlanacak. CameraX/Nav3/Coil3/KSP/AGP major/multimodule sırf yeni diye eklenmeyecek; Photo Picker/system Splash seçenekleri otomatik zorunlu değil.
- **23 — Güvenlik:** İstemci Firebase config ile gerçek sır ayrılacak. Maps literal anahtar uygun local/CI placeholder'a taşınacak; APK'da okunabilirliği ve gerçek package/SHA/API kısıtları ayrıca gözetilecek. Gemini gizli anahtarı BuildConfig/APK'dan çıkarılacak; service-account/keystore/parolalar Git'e konmayacak. Ignore/örnek config ve current/history değerlendirmesi yapılacak; canlı GitHub/Console doğrulanmamışken güvenli denmeyecek. Signing SHA public fingerprint ile private key karıştırılmayacak; yayınlanmayacak proje için release kurulum zorunlu değil. Rotation/history rewrite ayrı yetki ister.
- **30 — AI Logic:** Firebase AI Logic/Gemini Developer API yoluna dependency, feature/ai/di istemci ve request/response uyarlamasıyla geçilecek; yeni özel backend yok. Kullanıcı Console ekranlarında Enabled/Spark görüldü; Android entegrasyonu/App Check/yanıt başarısı henüz doğrulanmış değildir. Yalnız eski Gemini BuildConfig alanı kaldırılacak, tüm BuildConfig değil. Sırf etkinleştirme için google-services.json değişmesi zorunlu varsayılmaz; bağlantı doğrulanır ve gerekirse config adımı ayrıca bildirilir.
- **30 — İş akışı:** Yerel kullanıcı mesajı/pending history, model request ve finansal snapshot ayrı sahipte olacak. Firestore sync model çağrısını gereksiz bekletmeyecek; send guard/request-reply/finally/cancellation/failed-retry doğru yönetilecek, raw exception AI cevabı olarak kaydedilmeyecek. Finansal özette ortak hesap kullanılır; veri kapsamı sessiz daraltılmaz, system instruction güvenilmeyen not/input'tan ayrılır. createdAt sync ile değişmez, history görünümü korunur; model sohbet hafızası/retention/limit özellikleri otomatik eklenmez.
- **30 — Console/maliyet/gizlilik:** Ücretsiz desteklenen model/kota ve veri kullanım koşulları açık seçilecek; sınırsız ücretsiz veya ücretli veri gizliliğiyle eşdeğer vaat yok. Ek App Check debug token/auth enforcement/API/config adımı kullanıcıya adım adım söylenecek; token Git'e konmaz. Billing/Blaze/Agent Platform/monitoring veya template-only otomatik açılmaz. App Check tek başına kullanıcı yetkilendirmesi veya maliyet garantisi değildir.
- **32 — SDK build uyumu:** AI/Auth dependency değişiklikleri gereken BoM/catalog/plugin ile birlikte yapılacak; kullanılan SDK'dan önce eski dependency körlemesine kaldırılmayacak. Kalan genel dependency temizliği Faz 5'te tamamlanır; README yazılmaz.

**Faz sınırı:** SDK erişimi değişir, çalışan sohbet/analiz/giriş özelliği veya ekran tasarımı değişmez. UI/system-provider etkileşim farkı gerektiren seçenek somut olarak sunulur; ücretli plan ve dış Console yetkisi kendiliğinden genişlemez.

### Faz 5 — Ekranlar, navigation ve uygulama geneli tutarlılık

**Ana maddeler: 9, 11–14, 18–20 ve 31. Maddeler 8, 22 ve 32'nin kalan işleri.** Önceki feature veri ve adapter sınırları kullanılır.

- **9 — Uzun ekranlar:** Route VM/launcher/lifecycle toplama sahibi, Screen içerik state/callback tüketicisi olacak. İşlevi anlatan GoogleSignInButton gibi küçük component gerekirse Edit* bileşenini içeride kullanır, Edit* isimleri korunur. Form/photo/date/filter/list/bottom sheet gerçek sorumlulukta parçalanır; private aynı dosya ile feature components seçimi boyut/ownership üzerinden yapılır. Her buton için gereksiz dosya ve görünüşe göre dev component yok.
- **11 — Kod tutarlılığı:** Kotlin/import/visibility/ifade, sonuç API'si, modifier ve kaynak kullanımı eşdeğer durumlarda tutarlılaştırılacak. by/value örneği genelin yerine geçmez; UI state owners, nullability, booleans ve callback isimleri bütün dokunulan kodda incelenecek. Farklı işler aynı sentaksa zorlanmayacak.
- **12 — State:** Read-only VM state ve named actions, anlamlı ilişkili UiState, uygun Loading/empty/not-found/error sınırları düzenlenecek. Cold UI flow için ihtiyaca göre stateIn/WhileSubscribed; MutableStateFlow gereksiz yeniden sarılmayacak. Filter/combine tutarlı sahipte olacak; WhileSubscribed hesap verisini otomatik temizler veya background işi durdurur varsayılmayacak.
- **13 — Lifecycle:** Compose Flow collectAsStateWithLifecycle doğru yerlerde korunacak; alt bileşenlerde tekrar subscription/VM sahipliği dağıtılmayacak. repeatOnLifecycle yalnız manuel lifecycle toplama ihtiyacı varsa; her Compose ekranına eklenmez. Kritik sonucu tüketme sözleşmesi açık olur, Channel/event bus exactly-once garantisi sayılmaz.
- **14 — Callback:** Uygun imza ve salt forwarding varsa viewModel::action; ID binding/adaptation için lambda kullanılacak. Alt içerik/component VM yerine dar state/callback alacak. Business action sırası VM/uygun işlem sahibi, keyboard/navigation UI'da kalacak. Otomatik remember veya performans garantisi yok.
- **18 — Hatalar/metinler:** SDK failure → typed ortak/feature hata → presentation XML mesaj sınırı kurulacak; core/error, teknik SDK mapper ve feature domain/data/presentation ownership korunacak. İnternetsiz local saved/pending ile gerçek hata ayrılacak, raw exception kullanıcıya/AI cevabına aktarılmayacak. Field/snackbar/dialog görünümünü değiştiren sunum ayrıca onay ister; mevcut tasarım korunur. Aynı anlamlı mesaj ortak, farklı feature anlamı dev ErrorManager'a zorlanmaz.
- **19 — Tema/ölçü:** Statik kullanıcı metinleri XML'e, renkler aynı görsel sonucu veren semantic tema sahipliğine taşınacak. Gerçek ortak spacing core/ui/theme/Spacing.kt'de; bütün dp/global constant yapılmayacak. Typography/Shapes/gradient mevcut görünümle uyumlu tek sahipte olacak; dark/dynamic tasarım veya adaptive layout keyfi açılmaz.
- **20 — Navigation:** Navigation Compose Serializable typed destination/navigate/composable/toRoute/hasRoute kullanılacak; minimum ID taşınacak, entity/token parametre olmaz. App graphs ve feature state/callback ayrımı, auth/tab/back stack ve external link uyumu korunacak. Nav3 yok, GuideMate guide/tourist graph'ı kopyalanmaz; geri dönüş/saved-tab davranış farkı ayrıca onaylanır.
- **22 — Kalan XML/deep link/backup:** Cold/warm intent parser scheme/host/path/action doğrulama ve tek tüketimle typed hedefe bağlanacak. Reset oturumsuz, finansal hedef account readiness ile ele alınacak; mevcut URI bozulmayacak, sahip olunan HTTPS domain yoksa App Links varsayılmaz. Metin/tema/provider XML uyumu ve backup_rules/data_extraction_rules explicit politikası tamamlanacak; bütün backup aç/kapat ürün kararı keyfi seçilmez. Manifest referansları önceki fazlardan eksik bırakılmayacak.
- **31 — Performans/state/a11y:** Gereksiz grafik allocation/hesaplamalar ve kararlı lazy key uygun yerde düzenlenecek; zorunlu Paging/performance annotation yok. Nonvisual semantics ve state ownership korunacak; görünümü/akışı değiştiren font scale/layout/legend/touch target/IME çözümü ayrıca onay ister. İhtiyaçlı saved state ile Room kalıcılığı ayrılacak; bitmap/Context/büyük liste saved state'e konmayacak. AI global pendingAutoPrompt açık hesap/ekran/navigation sahibine taşınacak; otomatik yeni istek veya yeni draft davranışı eklenmeyecek.
- **8 — Kalan gerçek ortak yapılar:** Auth form/şifre alanı, anlamlı ortak UI, hata/tema gibi gerçek tekrarlar uygun sahipte tamamlanacak. Faz 2–4 iş kuralı tekrarları ile UI arasında yeniden duplicate workflow oluşturulmayacak.
- **32 — Kalan Gradle/config:** Duplicate Room/Activity/BoM pin'leri çözümlenen sürüm/kullanım üzerinden temizlenecek, direct dependency ve catalog tutarlı olacak. Kullanılmayan kütüphane sadece doğrulanmış kullanım yokluğunda kaldırılır. Wrapper/plugin/repository/ignore korunup sadeleştirilir; ilgisiz IDE kullanıcı değişiklikleri silinmez. Yayınlanmayacak proje için gereksiz signing/R8 kurulmaz. README en sonda; ilgili test kaynakları faz kapsamına dahildir.

**Faz sınırı:** Beş fazın refactor kapsamı uygulanmış olsa bile son test/cihaz doğrulaması/README yapılmış sayılmaz. Eksik/onay bekleyen alt işler ve dış ayarlar açık listelenir; kullanıcı son aşamayı ayrıca istediğinde başlatır.

### 35 maddenin kapsam eşlemesi ve tamamlanma sınırı

| Maddeler | Faz sahipliği |
|---|---|
| 1–7, 10 | Faz 1 temel; ilgili kod taşımaları ve kalite ilkeleri tüm fazlarda sürer |
| 8 | Faz 2 veri/iş kuralı; Faz 3–4 entegrasyon ortaklığı; Faz 5 UI/kalan tekrar |
| 9, 11–14 | Faz 5 |
| 15–17 | Faz 2 temel; Faz 3–5 ilgili işlerin doğru yaşam süresi/dispatcher kullanımı |
| 18–20 | Faz 5; typed error sınırları önceki veri/adapter fazlarında hazırlanabilir |
| 21 | Faz 4 ana; tarih/konum Faz 2–3, koşullu fotoğraf/splash ancak açık ihtiyaç ve koruma sınırıyla |
| 22 | Faz 3 cihaz/Startup/izin/provider; Faz 5 navigation/deep link/backup/XML kalan kapsam |
| 23 | Faz 4; gerekli ignore/güvenli config Faz 5 ile tamamlanır |
| 24–26 | Faz 2; 24 token/bildirim bağlantıları Faz 3 |
| 27–29 | Faz 3 |
| 30 | Faz 4; UI/global state bağlantısı Faz 5 |
| 31 | Faz 5 |
| 32 | Faz 4 gerekli SDK değişiklikleri; Faz 5 genel build/config temizliği; README ayrıca ertelendi |
| 33–34 | Baştaki Altın Kurallar ve Test Kalitesi, her faz öncesi kesinlikle okunur ve bütün fazlara uygulanır |
| 35 | Beş fazın ilgili testleri her fazda; genel son test taraması/cihaz doğrulaması/README kullanıcı ayrıca istediğinde |

Bu eşleme 1–35'in tamamını kapsar; her fazın gerekli testleri faza dahildir. Genel son test taraması/README/cihaz doğrulaması ve ayrı karar gerektiren ürün seçenekleri beş fazda otomatik tamamlanmış sayılmaz. Fazlar karışmasın diye alt işin ilk sahibi ve kalan bağlantısı açık tutulur; önceki madde ayrıntılarındaki kabul/istisnalar güncel Test Kalitesi kararıyla birlikte uygulanır.

## Çalışma sözleşmesi

- Bütün maddeler tek tek konuşulacak; netleşen kararlar ayrıntılarıyla bu belgeye eklenecek.
- Bütün maddeler tamamlanmadan üretim refactor'ına başlanmayacak. Bir maddenin kabulü, o maddeyi hemen kodda uygulama yetkisi değildir.
- Bu görüşme aşamasında yalnız karar/plan belgeleri düzenlenebilir. Üretim kodu, Gradle, Manifest, DB ve servis davranışı değiştirilmeyecek.
- Kullanıcı netleştirmesi: bütün maddeler tamamlanıp uygulama aşamasına geçildiğinde, onaylanan refactor için gerçekten gerekli kütüphane/Gradle/plugin ve Manifest/resource değişiklikleri de yapılacak. Gereksiz dependency/izin eklenmeyecek; sunucu/Console veya anahtar rotasyonu için ayrı yetki sınırları korunacak.
- Güncel kullanıcı kararı: her fazda ilgili mevcut testler incelenip gerekli testler yazılacak/uyarlanacak ve çalıştırılacak. Baştaki Test Kalitesi kuralları zorunludur. Son kapsam taraması/genel regresyon/cihaz kontrolleri ve README kullanıcı ayrıca istediğinde ele alınacak; önceki yalnız en sonda test kararı geçersizdir.
- Mevcut kullanıcı değişiklikleri korunacak; GuideMate Android/backend kodu bu FinanceAI kararı kapsamında değiştirilmeyecek.
- Over-engineering yok: yalnız gerçek sahiplik, bağımlılık, güvenilirlik veya okunabilirlik ihtiyacına dayanan değişiklikler.
- Kullanıcının bütün refactor için son sınırı: mevcut görsel tasarım değiştirilmeyecek; görünüm, renk, tipografi, yerleşim ve kullanıcı akışları korunacak. Amaç yeniden tasarım veya ürün davranışı değişimi değil, refactordur. Tema/spacing merkezileştirme aynı görünümü koruyacak. Önceden görüşülen hata düzeltmesi, responsive/a11y, veri kapsamı, zamanlama veya state restorasyonu mevcut görünen davranışı/tasarımı etkiliyorsa ayrıca somut etkisi açıklanıp kullanıcıyla netleştirilmeden uygulanmayacak; önceki genel madde onayları bu sınırı aşan otomatik izin sayılmaz.
- Kullanıcı ayrıca bütün maddeler için netleştirdi: dispatcher/interface/use case/ortak bileşen/merkezi yapı/paket ayrımı kullanmak için değil, gerçek sorumluluk, güvenilirlik, okunabilirlik veya test edilebilirlik ihtiyacını çözmek için eklenecek. Mevcut doğru kod korunacak; daha fazla yapı profesyonellik ölçüsü değildir.

## Karar durumu

5 Ekim uygulama notu: Faz 1 başladı; aşağıdaki karar kayıtları korunur. 1–7/10 yapısal temeli uygulanıyor, kapanış henüz yapılmadı. Maddelerin sonraki faza yayılan kapsamı tamamlanmış sayılmaz. Canlı dosya/test/temizlik ve kalan iş kanıtı docs/refactor-phase-1-progress.md içindedir. Diğer satırlardaki sonraki başlıklar geçmiş görüşme sırasıdır; uygulamada beş fazlı sıra esas alınır.

| Madde | Durum | Sonraki adım |
|---|---|---|
| 1 — MVVM, Clean Architecture ve Offline-First | Kullanıcı tarafından kabul edildi; aşağıdaki yedi karar kaydedildi. Faz 1 temel değişiklikleri uygulandı ve ilgili test/build doğrulandı; son kapsam değerlendirmesi sürüyor. | Faz 1 devam kaydındaki kalan kapsamı tamamla. |
| 2 — SOLID | Kullanıcı beş kararın tamamını kabul etti; ayrıntılar aşağıda kaydedildi. Faz 1 temel değişiklikleri uygulandı ve ilgili test/build doğrulandı; son kapsam değerlendirmesi sürüyor. | Faz 1 devam kaydındaki kalan kapsamı tamamla. |
| 3 — Bağımlılıkların doğruluğu | Kullanıcı yedi kararın tamamını kabul etti; ayrıntılar aşağıda kaydedildi. Faz 1 temel değişiklikleri uygulandı ve ilgili test/build doğrulandı; son kapsam değerlendirmesi sürüyor. | Faz 1 devam kaydındaki kalan kapsamı tamamla. |
| 4 — Test edilebilirlik | Kullanıcı altı kararın tamamını kabul etti; ayrıntılar aşağıda kaydedildi. Faz 1 temel değişiklikleri uygulandı ve ilgili test/build doğrulandı; son kapsam değerlendirmesi sürüyor. | Faz 1 devam kaydındaki kalan kapsamı tamamla. |
| 5 — Genişletilebilirlik | Kullanıcı beş kararın tamamını kabul etti; ayrıntılar aşağıda kaydedildi. Faz 1 temel değişiklikleri uygulandı ve ilgili test/build doğrulandı; son kapsam değerlendirmesi sürüyor. | Faz 1 devam kaydındaki kalan kapsamı tamamla. |
| 6 — İsimlendirmeler | Kullanıcı ortak Edit* bileşenlerini yeniden adlandırma önerisi hariç diğer kararları kabul etti. Faz 1 temel değişiklikleri uygulandı ve ilgili test/build doğrulandı; son kapsam değerlendirmesi sürüyor. | Faz 1 devam kaydındaki kalan kapsamı tamamla. |
| 7 — Paket yerleşimi ve ekran gruplandırılması | Kullanıcı yedi kararı kabul etti; ayrıntılar aşağıda. Faz 1 temel değişiklikleri uygulandı ve ilgili test/build doğrulandı; son kapsam değerlendirmesi sürüyor. | Faz 1 devam kaydındaki kalan kapsamı tamamla. |
| 8 — Kod tekrarı ve ortak yapılar | Kullanıcı yedi kararı kabul etti; ayrıntılar aşağıda. Uygulanmadı. | Sonraki maddeyi görüş. |
| 9 — Büyük ekranlar ve çoklu sorumluluklar | Kullanıcı altı kararı kabul etti; ayrıntılar aşağıda. Uygulanmadı. | Sonraki maddeyi görüş. |
| 10 — Fazla parametreli sınıf ve fonksiyonlar | Kullanıcı beş kararı kabul etti; ayrıntılar aşağıda. Faz 1 temel değişiklikleri uygulandı ve ilgili test/build doğrulandı; son kapsam değerlendirmesi sürüyor. | Faz 1 devam kaydındaki kalan kapsamı tamamla. |
| 11 — Kod tutarlılığı | Kullanıcı yedi kararı ve uygulama geneli tutarlılık ilkesini kabul etti. Uygulanmadı. | Sonraki maddeyi görüş. |
| 12 — UI state, stateIn ve WhileSubscribed | Kullanıcı altı kararı kabul etti. Uygulanmadı. | Sonraki maddeyi görüş. |
| 13 — Lifecycle-aware UI toplama | Kullanıcı beş kararı ve repeatOnLifecycle kullanım sınırını kabul etti. Uygulanmadı. | Sonraki maddeyi görüş. |
| 14 — Fonksiyon referansları ve lambda tercihleri | Kullanıcı beş kararı kabul etti. Uygulanmadı. | Sonraki maddeyi görüş. |
| 15 — Coroutine, scope ve cancellation | Kullanıcı altı kararı ve merkezi tanım/ayrı yaşam süresi açıklamasını kabul etti. Uygulanmadı. | Sonraki maddeyi görüş. |
| 16 — SupervisorJob ve ApplicationScope | Kullanıcı beş kararı kabul etti. Uygulanmadı. | Sonraki maddeyi görüş. |
| 17 — Dispatcher yönetimi | Kullanıcı beş kararı ve yalnız gerektiğinde kullanım ilkesini kabul etti. Uygulanmadı. | Sonraki maddeyi görüş. |
| 18 — Hata yönetimi ve XML mesajları | Kullanıcı yedi karar ve paket sahipliğini kabul etti. Uygulanmadı. | Sonraki maddeyi görüş. |
| 19 — Hardcoded metin, renk ve ölçüler | Kullanıcı altı karar ve Spacing.kt yerleşimini kabul etti. Uygulanmadı. | Sonraki maddeyi görüş. |
| 20 — Type-safe navigation ve back stack | Kullanıcı altı kararı ve GuideMate ile aynı API yaklaşımını kabul etti. Uygulanmadı. | Sonraki maddeyi görüş. |
| 21 — Ölçülü API modernizasyonu | Kullanıcı altı öneriyi koşullu seçenekler ve AI işlevini koruma sınırıyla kabul etti. Uygulanmadı. | Sonraki başlık 22 — Manifest, XML, izinler, deep link ve backup. |
| 22 — Manifest, XML, izinler, deep link ve backup | Kullanıcı altı öneriyi kabul etti; ayrıntılar aşağıda. Uygulanmadı. | Sonraki başlık 23 — API anahtarları, APK imzalama ve Git güvenliği. |
| 23 — API anahtarları, APK imzalama ve Git güvenliği | Kullanıcı altı öneriyi ve istemci yapılandırması/gizli anahtar ayrımını kabul etti. Uygulanmadı. | Sonraki başlık 24 — Oturum, hesap değişimi ve çıkışta veri koruma. |
| 24 — Oturum, hesap değişimi ve çıkışta veri koruma | Kullanıcı yedi öneriyi ve bildirimlerin hesap izolasyonunu kabul etti. Uygulanmadı. | Sonraki başlık 25 — Room, kayıt kimlikleri ve offline senkronizasyon. |
| 25 — Room, kayıt kimlikleri ve offline senkronizasyon | Kullanıcı yedi öneriyi kabul etti. Uygulanmadı. | Sonraki başlık 26 — Para, bütçe ve tarih hesaplamaları. |
| 26 — Para, bütçe ve tarih hesaplamaları | Kullanıcı yedi öneriyi kabul etti; para birimi ve yüzde anlamı uygulama öncesi netleştirilecek. Uygulanmadı. | Sonraki başlık 27 — Planlanan işlemler, WorkManager ve FCM. |
| 27 — Planlanan işlemler, WorkManager ve FCM | Kullanıcı sekiz öneriyi kabul etti; zamanlama/erteleme/sona erme ürün kararları açık kaldı. Uygulanmadı. | Sonraki başlık 28 — Fotoğraf, kamera ve dosya yönetimi. |
| 28 — Fotoğraf, kamera ve dosya yönetimi | Kullanıcı yedi öneriyi ve ortak medya hazırlama/ölçülü sıkıştırmayı kabul etti. Uygulanmadı. | Sonraki başlık 29 — Konum ve harita akışı. |
| 29 — Konum ve harita akışı | Kullanıcı yedi öneriyi kabul etti. Uygulanmadı. | Sonraki başlık 30 — Yapay zekâ entegrasyonu. |
| 30 — Yapay zekâ entegrasyonu | Kullanıcı Firebase AI Logic geçişi dahil sekiz öneriyi kabul etti. Console ek ayarları ayrıca bildirilecek. Uygulanmadı. | Sonraki başlık 31 — Performans, erişilebilirlik ve state restorasyonu. |
| 31 — Performans, erişilebilirlik ve state restorasyonu | Kullanıcı yedi öneriyi tasarım ve kullanıcı akışını koruma şartıyla kabul etti. Uygulanmadı. | Sonraki başlık 32 — Gradle, bağımlılıklar, release ve dokümantasyon. |
| 32 — Gradle, bağımlılıklar, release ve dokümantasyon | Kullanıcı yedi öneriyi kabul etti; README tüm refactor bittikten sonra hazırlanacak. Uygulanmadı. | Sonraki başlık 33 — Profesyonel kod kalitesi ve over-engineering sınırı. |
| 33 — Profesyonel kod kalitesi ve over-engineering sınırı | Kullanıcı yedi ilkeyi kabul etti; belgenin başında Altın Kurallar olarak kaydedildi. Uygulanmadı. | Sonraki başlık 34 — Açık ürün ve davranış kararları. |
| 34 — Açık ürün ve davranış kararları | Kullanıcı yedi karar/onay ilkesini kabul etti; baştaki Altın Kurallar'a eklendi. Açık ürün seçenekleri otomatik seçilmedi. | Sonraki başlık 35 — Uygulama fazları ve son doğrulama sırası. |
| 35 — Uygulama fazları ve son doğrulama sırası | Beş fazlı plan ve faz başına ilgili test kararı kayıtlı. Faz 1 üretim uygulaması başladı, kapanmadı; Faz 6 yok, genel son tarama/cihaz kontrolü/README ayrıca kullanıcı talebine ertelendi. | Her faz öncesi Altın Kurallar ve Test Kalitesi'ni oku, güncel dosya/test planı sun; güncel Faz 1 devam kaydından ilerle. |

Kaynak: [Kapsamlı denetim raporu](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/docs/refactor-audit.md), [devam kaydı](/Users/ahmetkaragunlu/AndroidStudioProjects/FinanceAI/docs/refactor-audit-progress.md).

## Madde 1 — Mimari: MVVM, Clean Architecture ve Offline-First

Durum: Kullanıcı, sunulan yedi mimari öneriyi kabul etti. Bu yalnız tasarım kararıdır; üretim uygulaması başlamadı.

### 1.1 Mevcut durum ve gerekçe

- MVVM/Compose/Hilt/Room/Flow altyapısı mevcut ve korunabilir.
- FinanceRepository arayüzü Room entity'leri döndürüyor; AuthRepository, Firebase AuthResult ve GoogleSignInAccount tiplerini taşıyor.
- Auth data implementasyonu presentation tarafındaki exception'a bağlı.
- Bazı ViewModel'ler local kayıt, Firebase sync, worker ve medya koordinasyonunu birlikte yapıyor.
- Yerel kayıt/anlık sync var; kalıcı pending işlem/silme, hesap izolasyonu ve conflict/reconciliation eksikleri dayanıklı offline-first hedefini zayıflatıyor.
- Asıl hedef framework değiştirmek değil; sorumluluk ve veri güvenilirliği sınırlarını netleştirmek.

### 1.2 Kabul edilen karar A — MVVM ve mevcut altyapı korunacak

- MVVM, Jetpack Compose, Hilt, Room, Coroutines/Flow ve WorkManager korunur.
- Tek app modülü yeterlidir; bu refactor için zorunlu multi-module geçişi yapılmaz.
- Uygulama baştan yazılmaz. Mevcut doğru lifecycle/state/repository davranışları gereksiz yere değiştirilmez.

### 1.3 Kabul edilen karar B — Presentation/domain/data sınırları

| Katman | Sahip olduğu işler | Taşımayacağı bağımlılıklar/sorumluluklar |
|---|---|---|
| Presentation | Route/Screen, ViewModel, UI state, UI metin eşlemesi ve kullanıcı etkileşimi | Doğrudan DAO/Firestore erişimi, local/cloud veri sırası koordinasyonu |
| Domain | Uygulama iş modelleri, ilgili repository sözleşmeleri, gerçek ortak veya karmaşık iş kuralları | Firebase/Room SDK tipleri, Context, Android resource ID, Activity/launcher |
| Data | Room/Firebase veri kaynakları, SDK adapter'ları, entity/DTO mapper'ları, veri işlemlerinin uygulanması | Screen/ViewModel exception veya UI bileşenine bağımlılık |

- Mantıksal bağımlılık presentation → domain ← data; DI implementasyonları bağlar.
- Her feature'a boş domain klasörü açılmaz.
- Her repository metoduna yalnız forwarding yapan use case eklenmez.
- Çok adımlı/ortak iş kuralında use case veya anlamlı işlem sınıfı kullanılabilir; somut ihtiyaç gerekçesi olmalı.

### 1.4 Kabul edilen karar C — ViewModel ekran davranışını yönetir

- ViewModel UI state'i üretir ve kullanıcı taleplerini ilgili repository/işlem sınırına iletir.
- ViewModel local kayıt → Firebase gönderme → sync flag → worker oluşturma sırasını ayrı ayrı koordine etmez.
- Yerel değişiklik ve gönderme niyetinin birlikte korunması data/ilgili işlem sınırında sahiplenilir.
- Loading, kayıt başarısı/hatası ve yeniden deneme gibi ekrana ait sonuçlar ViewModel tarafından UI state'e yansıtılır.
- UI'a ait navigation, ActivityResult launcher ve sheet/map animasyonu yalnız mimariyi kısaltmak için ViewModel'e taşınmaz.

### 1.5 Kabul edilen karar D — Alt bileşenlere state ve callback

- ViewModel screen/destination Route seviyesinde tutulur.
- Form, kart, dialog, sheet gibi alt bileşenler ViewModel nesnesi değil ihtiyaç duyduğu state ve callback'leri alır.
- Callback imzası uyumlu ve yalnız forwarding varsa viewModel::on... fonksiyon referansı ortak tercih olur.
- Argüman dönüşümü, event oluşturma veya UI'a ait birden fazla iş varsa anlamlı lambda korunur.
- ViewModel'e ait state, dışarıdan public mutable alan ataması yerine açık named action ile değiştirilir.
- Saf yerel UI state'inin tamamı zorunlu olarak ViewModel'e taşınmaz.
- Ayrıntılı bütün ekran callback/state dönüşüm listesi ilgili state/parçalama maddelerinde netleştirilecek.

### 1.6 Kabul edilen karar E — SDK modelleri üst katmandan ayrılır

- Room entity ve Firestore DTO data içinde kalır; ilgili mapper'larla uygulamanın ihtiyaç duyduğu modele dönüştürülür.
- Repository sözleşmelerindeki AuthResult/GoogleSignInAccount gibi SDK bağımlılıkları gerekli uygulama sonucu/payload sınırlarıyla ayrılır.
- UI kaynak mapper'ı ve exception'ı data katmanına bağımlılık olarak taşınmaz.
- Her katmanda aynı alanlara sahip zorunlu üç model oluşturulmaz; ayrı modelin gerekçesi bağımlılık, veri anlamı veya UI projection ihtiyacıdır.
- Local/remote ID, persisted enum/Firestore field ve mevcut kayıt ilişkileri model taşıması sırasında keyfi değiştirilmez.

### 1.7 Kabul edilen karar F — Finansal ekranların okuma kaynağı yerel veri

- Finansal ekranlar repository üzerinden Room'daki yerel veriyi izler.
- Firebase değişiklikleri yerel veriye uygulanır; UI doğrudan Firestore snapshot dinlemez.
- Room, finansal UI'ın yerel okuma kaynağıdır; bu, her çok cihazlı invariant'ın istemci tarafından güvenle çözülebileceği anlamına gelmez.
- Authentication ve AI çağrıları internet gerektirebilir; onları zorla Room/offline işlem gibi göstermeyiz.
- Offline-first ifadesi uygulamanın her özelliğinin offline çalışacağı garantisi değildir.

### 1.8 Kabul edilen karar G — Gönderme/silme niyeti kalıcı

- Yerel değişiklik ile bekleyen sync işlemi birlikte kalıcı kaydedilir.
- Gönderme, silme ve tekrar deneme ekranın veya ViewModel'in açık kalmasına bağlı olmaz.
- Dayanıklı senkronizasyon için kalıcı pending işlem/outbox ve uygun WorkManager yürütmesi hedeflenir.
- Conflict, version/opId, tombstone, hesap izolasyonu, backoff/permanent failure ve medya sırası ayrıntıları ilgili maddelerde ayrıca kararlaştırılır.
- Bu mimari onay, belirli konflikt politikası veya logout veri silme davranışının ayrıca onaylandığı anlamına gelmez.

### 1.9 Feature-first terimi ve GuideMate karşılaştırması

- Feature-first ve özellik/feature bazlı paketleme bu görüşmede aynı anlamda kullanılır: önce özellik sahipliği, sonra o özelliğin data/domain/presentation ayrımı.
- Mimari tanımı: MVVM + ölçülü Clean Architecture + Offline-First; paketleme hedefi feature-first.
- GuideMate'in mevcut Android checkout'unda auth/tour/payment gibi özellik köklerinde data/domain/presentation ayrımı doğrulandı. FinanceAI hedefi aynı temel sahiplik yaklaşımıdır; birebir klasör/kod kopyası değildir.
- GuideMate'in Spring Boot/API/token/payment mimarisi FinanceAI'ye otomatik taşınmaz. FinanceAI Room/Firebase ve dayanıklı offline sync ihtiyaçlarına göre düzenlenir.
- Feature-first için özelliklerin mutlaka feature/ adlı tek bir parent klasör altında olması gerekmez; önemli olan teknoloji yerine özellik sahipliğinin birincil ayrım olmasıdır.
- Kesin kök/alt paket adları, shared/core sahipliği ve hangi dosyanın nereye taşınacağı paketleme maddesinde netleştirilecek. Şimdiki örnek ağaçları nihai dosya taşıma onayı değildir.

### 1.10 İleride etkilenecek alanlar — ilk kapsam haritası

Bu liste henüz uygulanacak kesin dosya diff'i değildir; sonraki maddelerle tamamlanır.

| Alan | Mevcut kaynak/sahiplik | Hedef değişiklik |
|---|---|---|
| Auth | firebaserepo/AuthRepository ve AuthRepositoryImpl; screens/auth | SDK'sız uygulama sözleşmesi, data→UI exception bağımlılığının kaldırılması |
| Finans/bütçe | roomrepository/financerepository, budgetrepositroy | Domain sözleşmesi/modeli; local/cloud koordinasyonunun repository/işlem sınırına alınması |
| AI | ai_repository ve screens/main/aichat | Model client, finansal hesaplama ve mesaj verisi sahipliğinin ayrılması |
| Sync | firebasesync/FirebaseSyncService | Session/mapper/data-source/pending work/media/reminder sınırları; ayrıntılar diğer maddelerde |
| Room | roomdb/entity/DAO/converters/database; mevcut klasör adı entitiy | Entity/data sahipliği; yerel okuma kaynağı; migration planıyla uyum |
| UI | auth/main/location ekranları ve alt bileşenler | Route/ViewModel sınırı + state/callback; direct SDK/entity bağımlılığının ölçülü kaldırılması |
| DI/work/media/location | di/module, photo, notification, fcm, location | Yeni gerçek sözleşmelerin bağlanması; SDK erişiminin doğru sahipte kalması |

### 1.11 Korunacak davranışlar ve sınırlar

- Kayıtlı finans verisi, kimlikler, fotoğraf referansları ve persisted remote/link sözleşmesi korunmalı.
- Migration gerektiren değişiklik, yalnız paket/model taşıması gibi ele alınmamalı.
- Para birimi, expiration, reminder saati/tekrar, Google mevcut hesap politikası, logout pending veri ve AI paylaşım davranışı ilgili maddelerde kullanıcıyla netleştirilmeli.
- Yeni sunucu, ödeme, döviz veya özel kamera özelliği bu mimari karar nedeniyle eklenmez.
- Sunucu Rules/Console/key/history değişikliği bu tasarım onayının kapsamında değildir.

### 1.12 Over-engineering yapmama kararları

- BaseViewModel/BaseRepository/BaseScreen ağı yok.
- Her metoda forwarding use case yok.
- Her helper'a interface yok.
- Tek generic her-şeyi-yöneten manager yok.
- Zorunlu framework değiştirme, baştan yazma veya multi-module yok.
- Katman/model sayısını artırmak tek başına kalite ölçüsü değil; gerçek sorumluluk ve bağımlılık sorunu çözülmeli.

### 1.13 Uygulama zamanı ve tamamlanma ölçütü

- Bu madde uygulama bekliyor; bütün maddeler bitmeden uygulanmayacak.
- Tüm kararlar tamamlandığında küçük faz/dosya planı hazırlanacak; migration ve davranış değişimleri kozmetik taşımalardan ayrılacak.
- Hedef durumda finans UI okumaları repository/Room üzerinden; data→presentation bağımlılığı yok; ViewModel local/cloud sıra koordinasyonundan ayrılmış; reusable alt UI state/callback ile çalışıyor olmalı.
- Kayıtlı veriler ve hesap sınırları korunmalı; kalıcı sync niyeti tasarımı diğer kabul edilen maddelerle birlikte uygulanmalı.
- Test/derleme/lint/cihaz kabul sonuçları şu anda yoktur. Güncel kararla ilgili testler her faza dahil; genel son test taraması ayrıca yapılacak.

## Madde 2 — SOLID

Durum: 4 Ekim 2026'da kullanıcı, görüşülen beş SOLID kararının tamamını kabul etti ve kaydedilmesini istedi. Yalnız tasarım/plan kaydı; üretim koduna uygulanmadı.

### 2.1 Mevcut durum ve gerekçe

- FirebaseSyncService oturum/lifecycle, mapping, push/pull, listener, yerel mutasyon, medya, reminder ve Functions sorumluluklarını birleştiriyor.
- AiRepositoryImpl model çağrısı, finansal rapor/hesap, prompt üretimi ve local/cloud mesaj saklamayı birlikte yapıyor.
- AuthViewModel sign-in, sign-up ve password-reset ekranlarının farklı form/işlem state'lerini taşıyor.
- SyncType ayrımları birden fazla merkezi işlemde yeniden yapılıyor; yeni tür eklemek farklı sorumluluklara müdahale gerektiriyor.
- FinanceRepository transaction ve scheduled veri işlemlerini aynı geniş sözleşmede topluyor.
- AuthRepositoryImpl presentation AuthException'ına bağlı; bazı ViewModel'ler doğrudan FirebaseSyncService ve WorkManager ile veri akışını koordine ediyor.
- Belirgin bir Liskov ihlali kanıtlanmadı. Bütün SOLID harflerinde zorunlu sorun varmış gibi davranılmayacak.

### 2.2 Kabul edilen karar S — Tek sorumluluk

- Sınıflar satır sayısına değil, birbirinden bağımsız değişme nedenlerine ve gerçek sahipliğe göre ayrılacak.
- FirebaseSyncService içindeki oturum/listener koordinasyonu, tür mapper'ları, kalıcı sync yürütmesi, medya ve reminder davranışları anlamlı sahiplerine dağıtılacak.
- Yeni her sorumluluk için otomatik yeni manager oluşturulmayacak; mevcut ilgili altyapı yeterliyse geliştirilecek.
- AI model erişimi, finansal hesaplama/rapor ve mesaj verisi sahipliği ayrılacak. AiClient, financial report/snapshot ve ChatRepository gibi isimler adaydır; kesin dosya adları sonraki kapsam planında belirlenir.
- Sign-in, sign-up ve password-reset ekranlarının form/işlem state sahipliği ayrılacak; ortak auth repository ve session davranışı korunacak.
- Her dialog veya küçük alt bileşen için ayrı ViewModel oluşturulmayacak.
- Scheduled completion gibi tekrar eden ortak iş tek sahibi olan işlem olarak yönetilecek; ayrı ekran/receiver kopyaları korunmayacak.

### 2.3 Kabul edilen karar O — Gerçek genişleme noktaları

- Türün mapper ve veri davranışı ilgili sahipte toplanacak; ortak koordinatör entity alanlarının ayrıntılarını bilmeyecek.
- Yeni sync/özellik türü eklerken ilgisiz ekran/veri kurallarını değiştirme ihtiyacı azaltılacak.
- Bir when ifadesinin varlığı tek başına OCP ihlali sayılmayacak; küçük ve açık tür seçimi kalabilir.
- Gelecekte belki lazım olur diye plugin sistemi, generic adapter registry veya gereksiz kalıtım kurulmayacak.
- Mevcut syncToFirebase gibi gerçek ortak altyapı helper'ları yararlıysa korunacak; bütün türler zorla tek generic mapper'a dönüştürülmeyecek.

### 2.4 Kabul edilen karar L — Davranış/sözleşme tutarlılığı

- Interface'in vaat ettiği davranış, implementasyon tarafından korunacak. Buradaki karar kanıtlanmış bir Liskov ihlalini düzeltme iddiası değil; sözleşmeleri netleştirme kararıdır.
- Başarı anlamı açık olacak: yerel kayıt tamamlandı mı, uzak sync tamamlandı mı, sync hâlâ pending mi?
- Yerel kayıt başarılı + sync bekliyor sonucu tam senkronizasyon başarısı gibi sunulmayacak.
- Insert sonucu gerçek kayıt kimliğini taşıyacak; yanlış/default ID ile sonradan update yapılmayacak.
- Bulunamayan kayıt, beklenen hata ve coroutine iptali için tutarlı davranış tanımlanacak.
- Sessiz fallback, boş catch veya Result.failure'ı başarı saymak sözleşmeyi bozmayacak.
- CancellationException normal kullanıcı/ağ hatasına çevrilmeyecek; iptal korunacak.
- Kesin sonuç/failure tipleri ve retry/status alanları hata/state/offline-sync maddeleriyle birlikte netleştirilecek.

### 2.5 Kabul edilen karar I — Anlamlı repository sınırları

- FinanceRepository'nin transaction ve scheduled sorumlulukları TransactionRepository ve ScheduledTransactionRepository gibi anlamlı alan sözleşmelerine ayrılacak.
- Budget ve chat sözleşmeleri kendi alanlarında kalacak; isimler/adaptasyonlar naming/paketleme maddeleriyle hizalanacak.
- Her repository metodu için ayrı interface ve her ekran için aynı veriyi yöneten ayrı repository oluşturulmayacak.
- Transaction add/history/detail aynı iş alanının sözleşmesini kullanabilir; yalnız UI ekranı farklı diye data/domain kopyalanmayacak.
- Scheduled → transaction completion gibi alanlar arası işlem tek açık işlem sınırında yönetilecek. Repository ayrımı Room atomikliğini veya çok cihazlı server invariant'ını bozmayacak.
- Transaction/query/aggregation için ek read sözleşmesi ancak gerçek tüketici/sorumluluk ihtiyacı varsa oluşturulacak.

### 2.6 Kabul edilen karar D — Bağımlılık yönü ve Hilt

- Data katmanının presentation AuthException/resource mapper gibi tiplere bağımlılığı kaldırılacak.
- Repository/işlem sözleşmeleri gerekli uygulama modelleri ve anlamlı sonuçlarla SDK'dan ayrılacak.
- ViewModel, local/cloud sırasını concrete FirebaseSyncService/WorkManager üzerinden koordine etmeyecek; ilgili uygulama sözleşmesini tüketip UI state üretecek.
- Model erişimi, konum, medya ve scheduling gibi değişebilir dış sınırda anlamlı dar sözleşme kullanılacak.
- Constructor injection ve Hilt ile implementasyonlar bağlanacak. Hilt varlığı tek başına DIP'nin sağlandığı kabul edilmeyecek; bağımlılığın yönü/tipi değerlendirilecek.
- Saf hesaplayıcı, küçük mapper veya basit helper için otomatik interface açılmayacak.
- Binds/Provides, feature/app/shared DI konumu ve Singleton/session yaşam süresi ayrıntıları bağımlılık/paketleme/coroutine maddelerinde ayrıca kararlaştırılacak.

### 2.7 İleride etkilenecek alanlar

| Mevcut alan | Kabul edilen hedef |
|---|---|
| firebasesync/FirebaseSyncService ve SyncType | Koordinasyon ile tür mapping/veri, medya ve reminder sahipliğini ayır; gerçek ortak helper'ları koru |
| ai_repository/AiRepository ve AiRepositoryImpl | SDK model erişimi, finansal rapor ve chat veri sınırlarını ayrıştır |
| screens/auth/AuthViewModel, AuthState ve ilgili auth ekranları | Ekran/form state sahipliği; ortak auth/session davranışını koru |
| roomrepository/financerepository/FinanceRepository ve implementasyonu | Transaction ve scheduled sözleşmeleri; DB transaction/invariant'larını koru |
| firebaserepo/AuthRepository ve AuthRepositoryImpl; AuthExceptions | Data→UI bağımlılığını kaldır; SDK'sız auth sonucu/failure sözleşmesi |
| AddTransaction/Detail/Schedule/Home/Budget/AI ViewModel'leri | Değişen sözleşmeleri tüket; iş/veri koordinasyonunu doğru sahipte tut |
| DI modülleri, worker/FCM/photo/location entegrasyonları | Yeni anlamlı sınırları bağla; callback/job/UID davranışı korunarak uyarlama |

Bu tablo kesin dosya taşıma veya yeni sınıf listesi değildir. Uygulamadan önce ilgili fazın küçük dosya planı çıkarılacak.

### 2.8 Korunacak davranışlar ve over-engineering sınırı

- Persisted local/remote kimlikler, Firestore alanları, mevcut veriler ve dış link sözleşmeleri rastgele değiştirilmeyecek.
- Sınıf/repository ayrımı atomik işlemi parçalayıp yeni veri yarışı yaratmayacak.
- SDK/resource bağımlılığını kaldırırken UI mesajları ve mevcut kullanıcı politikaları ayrıca kabul edilen kararlarla hizalanacak; sessiz ürün davranışı değişikliği yok.
- Dependencies bag, BaseRepository/BaseViewModel ağı, generic plugin/framework, her metoda use case veya her helper'a interface yok.
- Satır sayısı, constructor parametre sayısı veya when kullanımı tek başına ihlal kanıtı değil.
- Eski sonuçların yeni hesap/kayıt sürümüne uygulanması yalnız isim değiştirme ile çözülmüş sayılmayacak; session/offline-sync kararlarıyla birlikte ele alınacak.

### 2.9 Uygulama ve tamamlanma ölçütü

- Bütün maddeler tamamlanmadan bu kararların refactor'ına başlanmayacak.
- Hedefte sınıfların iş sahipliği açık; repository API'leri anlamlı alanlara ayrılmış; data→UI bağımlılığı kaldırılmış olmalı.
- Yerel kayıt/remote sync/pending durumu, hata, kimlik ve iptal davranışları sözleşmede açık ve implementasyonda tutarlı olmalı.
- Ortak iş kuralları tek sahipte, tür farklılıkları ilgili mapper/veri sınırında olmalı; yalnız kodu başka dosyaya taşımak yeterli değil.
- İlgili testler her fazda yazılıp çalıştırılacak; genel son test taraması ayrıca yapılacak. Bu kayıt test/build/lint başarısı iddiası değildir.

## Madde 3 — Bağımlılıkların doğruluğu

Durum: Kullanıcı yedi önerinin tamamını kabul etti. Yalnız tasarım kararıdır; üretim uygulaması başlamadı. Bu madde sınıf/katman/servis bağımlılıkları hakkındadır; Gradle kütüphane ve sürüm düzenlemeleri 32. maddede ele alınacak.

### 3.1 Mevcut durum ve gerekçe

- AuthRepositoryImpl, presentation tarafındaki AuthException'a bağımlı.
- AuthRepository sözleşmesi AuthResult ve GoogleSignInAccount; FinanceRepository sözleşmesi Room entity'leri taşıyor.
- AddTransaction, Detail ve Schedule ViewModel'leri FirebaseSyncService/WorkManager üzerinden veri koordinasyonu yapıyor; BudgetViewModel de sync servisine doğrudan bağlı.
- AiRepositoryImpl; model SDK'sı, mesaj ve transaction DAO'ları, budget repository, sync servisi, Context ve dispatcher bağımlılıklarını birlikte taşıyor.
- Bazı repository implementasyonları DI modüllerinde uzun constructor çağrılarıyla elle oluşturuluyor. Bu geçersiz bir Hilt kullanımı değildir; uygun constructor injection ile tekrar azaltılabilir.
- Bağımlılık sayısı veya Context kullanımı tek başına yanlışlık kanıtı değildir; sahiplik, yön ve yaşam süresi birlikte değerlendirilecek.

### 3.2 Kabul edilen karar A — Katman bağımlılık yönü

- Repository arayüzü ilgili feature'ın domain katmanında, implementasyonu data katmanında olacak.
- Presentation domain sözleşmelerini tüketir; data bu sözleşmeleri uygular. Domain implementasyonu bilmez.
- Data; ekran sınıfı, UI exception'ı veya presentation kaynak mapper'ına bağımlı olmayacak.
- Domain; Firebase/Room SDK tipleri, Android Context ve resource ID'lerinden bağımsız kalacak.
- Tek modül korunuyor: bu sınırlar şimdilik paket/API seviyesindedir; ayrı Gradle modülü zorunluluğu yok. Kesin paket yerleşimi 7. maddede belirlenecek.

### 3.3 Kabul edilen karar B — SDK'sız uygulama sözleşmeleri

- AuthResult, GoogleSignInAccount ve Room entity'leri uygulama/domain sözleşmelerinin dışına taşmayacak; gereken bilgiler anlamlı uygulama modeli, payload veya sonuçla aktarılacak.
- Google girişinin Android etkileşimi uygun UI/adapter sınırında tutulacak; domain içine Activity/launcher/SDK nesnesi taşınmayacak.
- Mapper'lar SDK/veri sınırında olacak. Her katmanda aynı alanların zorunlu model kopyası oluşturulmayacak.
- Mevcut kimlikler, veri alanları ve Google giriş kullanıcı politikası bu ayrım gerekçesiyle değiştirilmeyecek. API modernizasyon ayrıntıları ilgili maddelerde ayrıca netleştirilecek.

### 3.4 Kabul edilen karar C — ViewModel'in altyapı bağımlılıkları

- ViewModel kaydet/sil/planı tamamla gibi talepleri ilgili repository veya gerçek ortak/karmaşık işi temsil eden işlem sınırına iletecek.
- Room yazımı, kalıcı pending işlem, sync ve worker planlama sırasını ViewModel yönetmeyecek; bu sorumluluk ilgili veri/işlem sahibinde olacak.
- Navigation, izin isteme ve launcher etkileşimi repository'ye taşınmayacak; UI sorumluluğu korunacak.
- Her repository metoduna forwarding use case açılmayacak. Mevcut doğru akışlar gereksiz soyutlamayla sarılmayacak.
- Bu değişiklik durable pending işlem/atomiklik/offline ve oturum kararlarıyla birlikte uygulanacak; yalnız servis çağrısını başka dosyaya taşımak yeterli değil.

### 3.5 Kabul edilen karar D — Feature'lar arası kontrollü bağımlılık

- Bir feature gerçek ihtiyacı olduğunda başka iş alanının domain sözleşmesini tüketebilir; her feature tamamen izole olmak zorunda değil.
- Başka feature'ın ViewModel'i veya repository implementasyonu kullanılmayacak. AI finans verisini ilgili sözleşme üzerinden alacak; başka alanın DAO'suna dayanıp iş kurallarını tekrar kurmayacak.
- Döngüsel bağımlılık kurulmayacak. Özellikle sync koordinatörü ile repository'lerin birbirini karşılıklı gerektirdiği yapı oluşturulmayacak; yerel veri erişimi/pending work sınırları uygun sahipte ayrılacak.
- Birden fazla yerde kullanılan her tip core'a taşınmayacak; gerçek ortak altyapı ile feature sahipliği ayrılacak.
- Scheduled completion gibi alanlar arası atomik işlem tek açık sahibini koruyacak; repository ayrımı yeni yarış veya kısmi başarı üretmeyecek.

### 3.6 Kabul edilen karar E — Hilt tanımlarını sadeleştirme

- Kendi sınıflarında mümkün olduğunda @Inject constructor kullanılacak.
- Arayüz–implementasyon eşlemesinde uygun olduğunda @Binds; Firebase/Room gibi SDK veya özel oluşturma gerektiren nesnelerde @Provides kullanılacak.
- Aynı tipin birden fazla anlamlı binding'i varsa açık qualifier kullanılacak; dispatcher qualifier'larının ayrıntıları 17. maddede.
- Feature binding'i ilgili feature sahipliğinde, ortak altyapı binding'i ortak alanda olacak. Uygulama entegrasyonu app tarafında kalabilir; kesin klasörler 7. maddede.
- Geçerli @Provides kullanımları sırf stil için topluca yanlış ilan edilmeyecek; constructor tekrarını azaltan dönüşümler yapılacak.
- Hilt modülü ile Gradle modülü aynı şey değildir; bu karar çok modüllü yapıya geçiş anlamına gelmez.

### 3.7 Kabul edilen karar F — Bağımlılıkların yaşam süreleri

- Her sınıfa otomatik @Singleton eklenmeyecek. Paylaşılan DB/SDK erişimi, ekran state'i ve hesap sahipliğindeki listener/job/cache farklı ihtiyaçlardır.
- Singleton nesne olması, hesabın verisi veya işlerinin uygulama boyunca korunması gerektiği anlamına gelmez.
- Hesap değişiminde gerekli iptal/temizlik ve eski callback sonuçlarının reddi açıkça yönetilecek; Hilt scope'u tek başına oturum güvenliği sağlamaz.
- Gerekmeden özel Hilt session component oluşturulmayacak. Session sahipliği ve coroutine yaşam süresi ayrıntıları 15–17 ve 24. maddelerde belirlenecek.
- Logout sırasında kalıcı verinin silinip silinmeyeceği bu maddeyle onaylanmış değildir; veri koruma politikası ayrıca kararlaştırılacak.

### 3.8 Kabul edilen karar G — Anlamlı soyutlama ve Context sınırı

- AI erişimi, konum, medya ve scheduling gibi değişebilir dış sınırlarda gereken kadar dar sözleşme kullanılacak.
- Saf hesaplayıcı, küçük mapper veya helper için otomatik interface oluşturulmayacak; BaseRepository/Dependencies bag/genel servis framework'ü kurulmayacak.
- Context topluca yasaklanmayacak: Android adapter/veri kaynağı gerçekten ihtiyaç duyduğunda uygun yaşam süresindeki Context'i kullanabilir.
- Domain Context/resource ID bilmeyecek; ViewModel kullanıcı mesajı üretmek için Context'e bağımlı olmayacak. Hata türü → XML mesajı eşlemesi presentation'da, ayrıntılar 18. maddede.
- Dispatcher injection ve main-safe çalışma sınırları 17. maddede detaylandırılacak; bu karar her suspend metoda gereksiz withContext ekleme yetkisi değildir.

### 3.9 Etkilenecek alanlar ve tamamlanma ölçütü

| Mevcut alan | Hedef |
|---|---|
| AuthRepository/Impl, AuthException, AuthViewModel | SDK'sız auth sözleşmesi; data→UI bağımlılığını kaldır; Android giriş adapter sınırı |
| Finance/Budget/AI repository sözleşmeleri ve implementasyonları | Domain API/data implementasyonu; feature sahipliği; SDK/entity sızıntısını kaldır |
| Add/Detail/Schedule/Budget ve ilgili ViewModel'ler | Veri/worker/sync koordinasyonunu doğru sahibine ilet |
| FirebaseSyncService ve local/pending work erişimi | Döngüsüz bağımlılık; UID/job sahipliği ve kalıcı işlem davranışı |
| DI modülleri, worker/service/receiver entegrasyonu | Uygun constructor/Binds/Provides/qualifier ve yaşam süresi |
| Konum, medya, AI adapter'ları | Gerçek dış sınır sözleşmeleri; Context ve SDK uygun katmanda |

- Uygulamadan önce faz bazında küçük dosya planı çıkarılacak; tüm maddeler bitmeden kod değiştirilmeyecek.
- Son yapıda bağımlılık yönleri, feature sahipliği, sözleşmeler ve yaşam süreleri uygulama genelinde yeniden kontrol edilecek. Bu onay tüm SOLID/bağımlılık sorunlarının şimdiden giderildiği iddiası değildir.
- Local/remote kimlik, atomiklik, veri koruma ve kullanıcı davranışları korunacak; ayrıca kabul edilen ürün kararları dışında sessiz değişiklik yapılmayacak.
- İlgili testler her fazda incelenip yazılacak ve çalıştırılacak. Bu kayıt test/build/lint başarısı iddiası değildir.

## Madde 4 — Test edilebilirlik

Durum: Kullanıcı altı tasarım kararını kabul etti. Test kaynakları incelenmedi; test yazımı ve çalıştırması yapılmadı. Bu madde üretim kodunun daha sonra doğrulanabilir tasarımı hakkındadır; tüm refactor bitmeden uygulanmayacak.

### 4.1 Mevcut durum ve gerekçe

- AddTransactionViewModel doğrudan sistem saati, Context, WorkManager ve FirebaseSyncService kullanıyor; kamera helper referansı tutuyor.
- Statik konum/fotoğraf erişimleri, somut SDK bağımlılıkları ve bağımsız scope'lar davranışın kontrollü koşullarda değerlendirilmesini zorlaştırıyor.
- Zaman, hesaplama, yan etki ve UI state sorumluluklarının ayrılması test edilebilirliği artıracak; mevcut testlerin yeterliliği bu incelemenin konusu değildir.

### 4.2 Kabul edilen karar A — Değiştirilebilir dış bağımlılıklar

- Repository, AI, konum, medya ve scheduling gibi gerçek dış sınırlar constructor üzerinden sağlanacak.
- İlgili iş davranışını gerçek Firebase, GPS veya dosya sistemine bağlanmadan doğrulayabilmek hedeflenecek. Gerçek adapter/entegrasyon davranışı için ileride uygun entegrasyon doğrulaması ayrıca gerekecek.
- Her küçük yardımcıya interface açılmayacak; anlamlı sözleşmeler 2 ve 3. maddelerle uyumlu olacak.

### 4.3 Kabul edilen karar B — Kontrol edilebilir zaman

- Şimdi, aylık dönem, son kullanma ve hatırlatma gibi iş hesabı gereken yerlerde enjekte edilen Clock ve açık zaman dilimi kullanılacak.
- Zaman aralığı sınırları ve date-only/instant ayrımı ilgili finans/zaman maddesinde netleştirilecek.
- Her tarih biçimlendirme fonksiyonuna ayrı saat sağlayıcı eklenmeyecek; saf formatlama ile zamanı okuyan iş kuralı ayrılacak.

### 4.4 Kabul edilen karar C — Açık coroutine çalışması ve tamamlanma

- Dispatcher gereken yerde enjekte edilecek; kontrolsüz bağımsız scope ve sonucu beklenmeyen işler azaltılacak.
- Tamamlanma, iptal, yerel başarı ve sync için sıraya alma birbirinden ayrılacak. Ekran ömrünü aşan işler uygun sahibi veya kalıcı iş mekanizmasıyla yönetilecek.
- viewModelScope korunacak; sırf test için özel ViewModel scope altyapısı kurulmayacak.
- Dispatcher/scope ayrıntıları 15–17. maddelerde; her suspend çağrıya gereksiz withContext eklenmeyecek.

### 4.5 Kabul edilen karar D — Saf iş hesapları

- Para doğrulama, bütçe yüzdesi, tarih aralığı ve finansal özet gibi hesaplar mümkün olduğunda girdiyi alıp sonucu döndüren küçük Kotlin yapıları olacak.
- Bu hesaplar Firebase, Compose, Context veya dosya erişimi gerektirmeyecek; veri edinme ve hesaplama sorumluluğu ayrılacak.
- Her hesap için ayrı use case veya interface zorunlu değil; gerçek ortak iş kuralı tek sahibinde kalacak.

### 4.6 Kabul edilen karar E — State/callback ile UI doğrulanabilirliği

- Route; ViewModel, navigation, permission ve launcher bağlantısını kuracak. Ekran içeriği/tekrar kullanılabilir alt bileşenler gereken state ve callback'leri alacak; tüm ViewModel aktarılmayacak.
- Loading, boş içerik, hata ve başarılı durumlar gerçek hesap/servis olmadan temsil edilebilecek.
- ViewModel kamera launcher/helper referansı tutmayacak; yalnız gereken medya sonucu/draft bilgisi alacak.
- Saf UI'ı gerekmeden parçalama veya bütün yerel UI state'ini ViewModel'e taşıma zorunluluğu yok. UI state ayrıntıları ilgili maddelerde netleşecek.

### 4.7 Kabul edilen karar F — Gözlemlenebilir sonuç ve state geçişleri

- Dışarıya salt okunur state; değişiklik için açık eylemler sunulacak.
- Yerel kayıt başarısı, bekleyen sync ve başarısızlık açık sonuç/state ile ayrılacak.
- Hata yutma, yanlış başarı ve teknik hatanın normal AI cevabı olarak saklanması giderilecek; coroutine iptali normal işlem hatasına çevrilmeyecek.
- Sonuç ve kullanıcı mesajı ayrımı 18. maddeyle; durable işlem ve oturum davranışı ilgili maddelerle hizalanacak.

### 4.8 Kapsam, sınırlar ve tamamlanma ölçütü

- Etkilenecek alanlar: ilgili ViewModel/Route/content, domain hesap/validation yapıları, repository/SDK adapter'ları, tarih/konum/medya erişimi, dispatcher ve yaşam süresi binding'leri.
- Üretim kodunda test modunda farklı davranış bayrağı, genel test framework'ü veya yalnız test gerekçesiyle gereksiz soyutlama oluşturulmayacak.
- Mevcut kimlikler, saklanan veri, offline davranış ve kullanıcı politikası korunacak; ürün değişiklikleri ayrıca onaylanacak.
- Tasarım kontrolünde dış sınırların sağlanabilirliği, zamanın kontrolü, saf hesapların ayrılığı, UI state/eylem sınırı ve işlem sonucunun açıklığı değerlendirilecek. Bu kriterler testlerin geçtiği anlamına gelmez.
- İlgili test kaynakları her fazda incelenip gerekli testler yazılacak ve çalıştırılacak. Bu tur test/build/lint çalıştırılmadı.

## Madde 5 — Genişletilebilirlik

Durum: Kullanıcı beş kararın tamamını kabul etti. Üretim kodu değişmedi. Hedef yeni davranış eklerken ilgisiz alanlarda zorunlu değişiklikleri azaltmak; gelecekteki belirsiz ihtiyaçlar için altyapı kurmak değil.

### 5.1 Mevcut durum ve gerekçe

- Raporda yeni sync türünün merkezi when bloklarının birçok yerine eklenmesi gerektiği tespit edildi.
- SDK tiplerinin sözleşmelere taşması, geniş sync/AI sınıfları ve farklı ekranlarda yinelenen iş kuralları değişikliğin yayılmasını artırıyor.
- İlk dört maddenin sahiplik, katman, bağımlılık ve test edilebilirlik kararları korunacak; genişletilebilirlik için ayrı bir genel framework oluşturulmayacak.

### 5.2 Kabul edilen karar A — Feature sahipliğinde değişiklik

- Transaction/budget gibi alanların yeni davranışları ilgili feature sahipliğinde geliştirilecek; başka feature'ın ekranına veya implementasyon ayrıntısına dayanılmayacak.
- Gerçek ihtiyaçta domain sözleşmeleri üzerinden feature iletişimi mümkün; tamamen bağımsız feature zorunluluğu yok.
- Ortaklaştırma sahipliği belirsizleştirmeyecek; kesin paket konumları 7. maddede.

### 5.3 Kabul edilen karar B — Sync türünün kendi mapping/veri sınırı

- Transaction, scheduled transaction, budget ve AI mesajı mapping/veri işlemleri ilgili sahiplerinde olacak.
- Ortak koordinatör oturum/sync akışını yönetecek; bütün kayıt türlerinin alanlarını bilmeyecek.
- Küçük ve açık when yönlendirmesi kalabilir. Generic adapter registry/plugin sistemi veya zorunlu kalıtım ağı kurulmayacak.
- Mevcut yararlı ortak helper'lar korunacak; durable pending işlem ve oturum detayları ilgili maddelerde netleştirilecek.

### 5.4 Kabul edilen karar C — Dış servis değişikliğini sınırda karşılama

- AI SDK veya konum/medya erişim yöntemi değişikliği mümkün olduğunca ilgili adapter ve DI bağlantısında karşılanacak; ekranlara gereksiz yayılmayacak.
- Servislerin farklı yeteneklerini aynıymış gibi gösteren aşırı genel interface oluşturulmayacak. Gerçek davranış farkında uygulama sözleşmesi gerektiği kadar değişebilir.
- SDK/API modernizasyon seçimi ilgili maddelerde; bu onay yeni servis veya ürün özelliği ekleme yetkisi değil.

### 5.5 Kabul edilen karar D — Ortak iş kuralının tek sahibi

- Para doğrulama, dönem hesabı ve scheduled completion gibi gerçekten ortak kurallar tek sahibinde geliştirilecek.
- Aynı tutar doğrulamasının Add ve Detail'de ayrı ayrı düzeltilmesi gerekmeyecek; UI kontrolü domain doğrulamasının yerine geçmeyecek.
- Benzer görünen fakat farklı anlam/yaşam süresi/yan etkiye sahip işlemler yalnız satır benzerliği nedeniyle birleştirilmeyecek.
- Ortaklaştırılacak somut tekrarlar 8. maddede; finans ve zaman kuralları 26, scheduled işlemler 27. maddede detaylandırılacak.

### 5.6 Kabul edilen karar E — Kontrollü UI ve kalıcı veri evrimi

- Ortak UI anlamlı state/callback ile genişletilecek; onlarca boolean üzerinden bütün ekranları yöneten genel bileşen kurulmayacak.
- Kalıcı alan/sync türü eklenirken Room migration, eski veri uyumu ve pending işlem biçimi birlikte değerlendirilecek.
- Paket/sınıf düzenlemesi persisted tür değerlerini, Firestore alanlarını veya worker kimliklerini farkında olmadan değiştirmeyecek; gereken değişiklik açık uyumluluk/geçiş planıyla ele alınacak.
- Bu karar veri şeması veya ürün davranışının hemen değiştirilmesi anlamına gelmez; ayrıntılar ilgili maddelerde onaylanacak.

### 5.7 Kapsam, korunacak davranış ve tamamlanma ölçütü

- Etkilenecek alanlar: feature API/implementasyon sınırları, FirebaseSyncService ve tür mapper/veri işlemleri, dış servis adapter'ları/DI, ortak validation/hesap/işlem sahibi, reusable UI ve kalıcı işlem uyumluluğu.
- Başarı ölçütü mevcut dosyaların hiç değişmemesi değil; değişikliğin doğru sahipte, sınırlı ve anlaşılır olmasıdır.
- Çok modüllü mimari, plugin framework, Base sınıf ağı veya geleceğe dönük gereksiz uzatma noktaları kurulmayacak.
- Veri kimlikleri, hesap izolasyonu, atomik işlemler ve mevcut kullanıcı politikası korunacak; farklı ürün davranışı ayrıca kararlaştırılacak.
- Tüm maddeler bitmeden refactor başlamayacak. İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur test/build/lint çalıştırılmadı.

## Madde 6 — İsimlendirmeler

Durum: Kullanıcı sunulan kararları, EditTextField/EditTopBar/EditButton gibi ortak bileşenler için FinanceTextField/FinanceTopAppBar/TransactionFilterButton adlarını öneren cümle hariç kabul etti. Hariç tutulan öneri bu madde kapsamında uygulanmayacak; bileşen adları için yeni bir karar alınmadı. Üretim kodu değişmedi.

### 6.1 Kapsam ve gerekçe

- Paket/dosya/sınıf/ekran/fonksiyon/parametre/değişken isimleri ele alınır. Dosyanın hangi katman/pakette duracağı 7. maddede netleştirilecek.
- Kaynaklarda entitiy, budgetrepositroy, DataPickerField, isRemenderEnabled yazım hataları; detay/AI/schedule için belirsiz isimler; bütçe ekranına giden AnalysisScreen hedefi bulunuyor.
- Amaç isimleri topluca uzatmak değil, doğru görevi anlatan tutarlı adlar kullanmak. Açık ve doğru adlar gereksiz değiştirilmeyecek.

### 6.2 Kabul edilen karar A — Yazım ve dosya/sembol tutarlılığı

| Mevcut | Kabul edilen hedef |
|---|---|
| entitiy | entity; nihai feature/data yerleşimi 7. maddede |
| budgetrepositroy | Nihai feature/budget/data/repository yerleşiminde yazım hatasını kaldır |
| DataPickerField.kt | DatePickerField.kt |
| isRemenderEnabled | isReminderEnabled |
| Database BudgetDao() metodu | budgetDao() |
| PhotoHelper.kt / CameraHelper | Nihai UI launcher/helper sorumluluğuna göre dosya ve sınıf adını tutarlı yap; kesin ad sonraki planlamada |

- Paket taşırken önce geçici isim düzeltmesi sonra tekrar taşıma yerine nihai yerleşime tek geçiş yapılabilir.

### 6.3 Kabul edilen karar B — İş alanını anlatan ekran/ViewModel adları

- DetailScreen → TransactionDetailScreen; DetailViewModel → TransactionDetailViewModel.
- AiViewModel → AiChatViewModel.
- ScheduleViewModel → ScheduledTransactionsViewModel.
- Budget ekranına giden AnalysisScreen destination'ı bütçeyi ifade eden adla değiştirilecek; type-safe model ve uyumluluk ayrıntısı 20. maddede.
- HomeViewModel/BudgetViewModel gibi zaten açık adlar sırf değişiklik olsun diye değiştirilmeyecek.

### 6.4 Kabul edilen karar C — Route/Screen/UiState/navigation ayrımı

- ViewModel bağlantısını kuran composable için TransactionDetailRoute gibi Route adı; state/callback ekran içeriği için TransactionDetailScreen gibi Screen adı kullanılacak.
- Ekran state'i TransactionDetailUiState gibi açık adla ifade edilecek.
- Type-safe navigation hedefi Route composable ile karışmayacak; TransactionDetailDestination örnek adaydır, kesin destination konvansiyonu 20. maddede belirlenecek.
- İsimlendirme bir ekran için zorunlu boş sınıf/dosya üretme gerekçesi değildir; gerçek sorumluluk ayrımı korunacak.

### 6.5 Kabul edilen karar D — Davranışı anlatan fonksiyon adları

- Sürekli veri izleyen Flow API'lerinde observeTransactions gibi observe; tek seferlik okumada getTransaction gibi get tercih edilecek.
- Kimlik üretimi generateTransactionId, hesaplama calculateBudgetUsage gibi gerçek davranışı ifade edecek.
- UI eylemleri onAmountChanged/onSaveClicked gibi adlandırılabilir; iş işlemi saveTransaction gibi kendi görevini anlatacak.
- İsimler mekanik kalıba sokulmayacak; getter, hesaplama, iş işlemi ve kullanıcı etkileşimi farkı korunacak.

### 6.6 Kabul edilen karar E — Servis/iş görevi ve açık istisna

- MyFirebaseMessagingService → FinanceMessagingService.
- DeleteExpiredNotification gibi işin nihai adı gerçekten hangi verinin hangi politikayla işleneceğine karar verildikten sonra belirlenecek; yanlış davranış yalnız ad değişikliğiyle gizlenmeyecek.
- Kullanıcının hariç tuttuğu ortak Edit* UI bileşenlerini yeniden adlandırma önerisi kabul listesinde değildir. EditTextField/EditTopBar/EditButton için önerilen FinanceTextField/FinanceTopAppBar/TransactionFilterButton dönüşümleri yapılmayacak; bu kapsam benzer ortak Edit* adlarını otomatik yeniden adlandırma yetkisi de vermez.
- Denetim raporundaki ilgili öneri tarihsel bulgu/öneri olarak kalabilir; uygulanacak kararın kaynağı bu belgedeki kullanıcı istisnasıdır.

### 6.7 Kabul edilen karar F — Tutarlılık ve kalıcı veri uyumluluğu

- Constructor alanlarında repo gibi belirsiz kısaltma yerine transactionRepository gibi anlamlı isim kullanılacak; kısa ve bağlamı açık lambda'da it geçerlidir.
- Birden fazla kimlik varsa localId/remoteId/userId ayrımı açık olacak.
- Boolean adında anlama göre is/has/can; koleksiyonlarda çoğul ad kullanılacak.
- Kaynak sembol değişikliği Firestore alanlarını, Room'da saklanan enum değerlerini, deep link'leri veya kalıcı worker kayıtlarını kontrolsüz değiştirmeyecek; gerekirse açık geçiş planı uygulanacak.

### 6.8 Uygulama sınırları ve tamamlanma ölçütü

- Etkilenecek alanlar: ilgili feature paket/dosya adları, ekran/ViewModel/state adları, navigation sembolleri, DAO/repository fonksiyonları, kamera helper dosya/sembol uyumu, FCM servis ve davranışı netleştirilen worker adları, parametre/alan isimleri.
- Kesin dosya taşıma ve import planı 7. madde/faz planında; type-safe navigation ve worker veri uyumluluğu ilgili maddelerde ayrıca netleştirilecek.
- Kullanıcı istisnası korunacak; bütün maddeler bitmeden üretim refactor'ı başlamayacak.
- Tamamlanmada yazım, dosya/sembol uyumu, görev açıklığı ve çağrı noktalarının tutarlılığı kontrol edilecek; kalıcı sözleşmelerin korunması doğrulanacak.
- İlgili testler her fazda incelenip yazılacak ve çalıştırılacak. Bu tur yalnız belgeler değişti; test/build/lint çalıştırılmadı.

## Madde 7 — Paket yerleşimi ve ekran gruplandırılması

Durum: Kullanıcı yedi yerleşim kararını kabul etti. Tek Gradle modülü korunur; bu paket/API sahipliği kararıdır. Dosya taşınmadı, üretim kodu değişmedi. Profesyonel hedef yalnız klasör adlarıyla değil doğru sorumluluk, bağımlılık ve uygulama tutarlılığıyla değerlendirilecek; tüm uygulamanın üretime hazır olduğu iddiası değildir.

### 7.1 Mevcut durum ve gerekçe

- screens/main görünüm yerleşimi, roomrepository/firebaserepo gibi kökler teknoloji ağırlıklı ayrım yapıyor; mevcut feature paketi çoğunlukla navigation kodu içeriyor.
- Detail ekranı history altında; FinanceApplication di/application altında. Bunlar hedef sahiplikle düzenlenecek.
- Her feature'a aynı boş klasörleri açmak yerine gerçek içerik ve sorumluluk esas alınacak. İsimlendirme kararları ve kullanıcının Edit* istisnası korunacak.

### 7.2 Kabul edilen karar A — app/core/feature kökleri

```text
com.ahmetkaragunlu.financeai/
├── app/
│   ├── navigation/
│   └── di/
├── core/
│   ├── database/
│   ├── sync/
│   ├── media/
│   ├── time/
│   └── ui/
│       ├── theme/
│       └── components/
└── feature/
    ├── auth/
    ├── transaction/
    ├── schedule/
    ├── budget/
    ├── home/
    ├── aichat/
    └── location/
```

- Özellikler dıştaki feature paketi altında bulunacak; feature-first/özellik bazlı terimleri aynı anlamda kullanılır.
- Bu paket ağacı çok modüllü mimari değildir. İçeriği gerekmeyen klasörler boş oluşturulmayacak.

### 7.3 Kabul edilen karar B — Ekranların presentation yerleşimi

- Auth ekranları feature/auth/presentation/signin, signup ve passwordreset altında gruplanacak.
- Password-reset request ve yeni parola ekranları aynı akış olarak passwordreset altında birlikte bulunabilir; gerçek büyüklük ihtiyacı yoksa ek alt paket zorunlu değil.
- Transaction ekranları feature/transaction/presentation/add, history ve detail altında olacak. Detail history'nin altında kalmayacak; işlem detayı yalnız geçmiş listesine ait değil.
- Gereken Route/Screen/ViewModel/UiState ilgili ekran paketinde bulunacak. Boş sınıf/dosya veya her küçük bileşene ayrı paket oluşturulmayacak.
- Budget/home/aichat/schedule/location ekranları kendi feature presentation katmanında olacak. Diğer alt paketler gerçek içerik büyüklüğüne göre belirlenecek.

### 7.4 Kabul edilen karar C — İş alanı başına domain/data

```text
feature/transaction/
├── domain/
│   ├── model/
│   └── repository/TransactionRepository.kt
├── data/
│   ├── local/
│   ├── remote/
│   ├── mapper/
│   └── repository/TransactionRepositoryImpl.kt
├── presentation/
│   ├── add/
│   ├── history/
│   └── detail/
└── di/
```

- Add/history/detail aynı iş alanının domain/data sözleşmelerini paylaşacak; ekran başına kopya katman oluşturulmayacak.
- Repository arayüzü domain, implementasyonu data katmanında kalacak; SDK/entity tipleri sözleşmeye sızmayacak.
- usecase/validation/di gibi alt paketler gerçek içerik varsa bulunacak; her feature'a zorunlu üç katman veya her metoda use case yok.
- Home başka alanların verisini gösteriyorsa presentation ile kalabilir; simetri için HomeRepository oluşturulmayacak.

### 7.5 Kabul edilen karar D — Veri sahipliği ve ortak DB

- Transaction DAO/entity → feature/transaction/data/local.
- Scheduled DAO/entity → feature/schedule/data/local.
- Budget DAO/entity → feature/budget/data/local.
- AI mesaj DAO/entity → feature/aichat/data/local.
- FinanceDatabase, migration ve ortak DB kurulumu → core/database; bu composition feature local tiplerini bir araya getirebilir, feature API'lerinde DB implementasyon ayrıntısı açılmaz.
- Transaction ve ilgili iş modelleri alan sahibinde kalacak; home/AI kullanıyor diye tüm modeller core/model içine taşınmayacak.
- Converters/kalıcı enum yerleşimi anlam ve kullanımına göre dosya planında belirlenecek; saklanan değerler kontrolsüz değiştirilmeyecek.

### 7.6 Kabul edilen karar E — Ortak UI ve feature bileşenleri

- Birden fazla feature'da aynı anlamla kullanılan UI core/ui/components; budget'a özel kartlar feature/budget/presentation/components altında olacak.
- ExpensePieChart, kategori ikon/metin eşlemeleri ve dropdown yapıları gerçek kullanım/sahipliğe göre yerleştirilecek; topluca utils klasörüne atılmayacak.
- EditTextField/EditTopBar/EditButton gibi bileşenler uygun ortak pakete taşınabilir fakat kullanıcının 6. madde istisnasıyla isimleri otomatik değiştirilmez.
- Bileşenin yalnız küçük olması veya iki dosyada kullanılması tek başına core'a taşıma gerekçesi değildir; ortak anlam ve değişim sahipliği değerlendirilecek.

### 7.7 Kabul edilen karar F — Sync/medya/konum/bildirim sahipliği

- Genel outbox/sync yürütme altyapısı core/sync; kayıt türüne özgü mapper ve local/remote işlemler ilgili feature data katmanında olacak.
- Ortak fotoğraf/dosya altyapısı core/media; launcher bağlantısı ilgili UI Route'unda kalacak.
- Konum seçimi feature/location/presentation, konum/geocoding adapter'ları ilgili data sınırında olacak. Domain sözleşmesi/modeli gerçekten gerekiyorsa eklenecek; boş domain zorunlu değil.
- Plan tamamla/ertele/hatırlat iş davranışı schedule alanına ait olacak; transaction formu plan oluşturma talebini ilgili sözleşmeye iletebilir, plan kurallarını kopyalamaz.
- FCM servis giriş noktası ve uygulama düzeyindeki yönlendirme app entegrasyonunda; gerçekten genel bildirim altyapısı gerektiğinde core'da bulunacak.
- Teknik olarak bildirimle ilişkili her sınıf aynı pakete toplanmayacak; iş kuralı, genel mekanizma ve app giriş noktası ayrılacak.
- Sync/session/offline ve FCM/hatırlatma detayları ilgili maddelerde netleşecek; bu yerleşim onayı server/Console veya ürün politikası değişikliği yetkisi değildir.

### 7.8 Kabul edilen karar G — DI ve uygulama giriş noktaları

- MainActivity, FinanceApplication, ana navigation/shell ve deep-link koordinasyonu app altında olacak; FinanceApplication di/application altında kalmayacak.
- Feature repository binding'leri feature/di; ortak altyapı binding'leri kendi altyapısının yanında; uygulama entegrasyon binding'leri app/di içinde bulunacak.
- Constructor injection yeterli olduğunda ek DI dosyası açılmayacak. Yerleşim scope anlamına gelmez; yaşam süreleri ilgili kararlarla belirlenecek.
- FinanceNavigation default package'ta kalmayacak; uygulama navigation sahipliğinde olacak.
- Taşıma sırasında Manifest sınıf yolları, worker kayıtları ve diğer kalıcı referanslar kontrol edilecek; gerekiyorsa açık uyumluluk/geçiş planı kullanılacak.

### 7.9 Dosya planı ve tamamlanma ölçütü

- Hedef her dosyanın açık sahibi/doğru katmanı olmasıdır; bütün paketlerin aynı klasörlere sahip olması değil.
- Faz uygulaması öncesi ilgili kaynaklar için dosya bazlı taşıma/ayırma ve import planı çıkarılacak; büyük sınıf parçalama kararları ilgili maddelerle birlikte ele alınacak.
- Package taşıması applicationId, Firestore alanı, DB tablo/enum değeri, deep link veya kalıcı worker kimliğini farkında olmadan değiştirmeyecek.
- Üretim refactor'ı tüm maddeler bitmeden başlamayacak. Bitince yerleşimler ve gerçek bağımlılık yönleri uygulama genelinde yeniden kontrol edilecek; klasör düzeni tek başına kalite kanıtı kabul edilmeyecek.
- Edit* kullanıcı istisnası korunacak. Mevcut kullanıcı değişikliklerine/GuideMate koduna dokunulmayacak.
- İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur yalnız karar belgeleri değişti, test/build/lint çalıştırılmadı.

## Madde 8 — Kod tekrarı ve ortak yapılar

Durum: Kullanıcı yedi kararı kabul etti. Yalnız aynı anlam ve davranışı temsil eden gerçek tekrarlar ortaklaştırılacak; satır benzerliği yeterli değil. Üretim kodu değişmedi.

### 8.1 Mevcut durum ve gerekçe

- ScheduleViewModel ve NotificationActionReceiver ayrı transaction insert/scheduled delete akışları içeriyor; kimlik ve medya davranışları farklılaşmış.
- Add/Detail/Budget tutar doğrulaması ve finansal dönem/özet hesaplarında tekrar ve tutarsızlık adayları var.
- Fotoğraf/hatırlatma worker oluşturma ve iptal bilgileri birden fazla çağrı noktasında kuruluyor.
- FirebaseSyncService pushUnsynced, update ve delete blokları benzer fakat türlerin medya, sync sonucu ve bildirim yan etkileri farklı. Mevcut syncToFirebase zaten yararlı ortak yardımcı içeriyor.
- Auth/işlem form parçaları, hata/metin ve tema değerleri için gerçek ortaklıklar var; bütün benzer kart/formların birleştirilmesi gerekmiyor.

### 8.2 Kabul edilen karar A — Tek plan tamamlama işlemi

- ScheduleViewModel ve NotificationActionReceiver tek açık scheduled completion işlemini çağıracak.
- İşlem oluşturma, planın işlenmesi/silinmesi, kimlik, medya, bildirim iptali ve sync niyeti aynı iş sahibinde koordine edilecek.
- Atomiklik, idempotence ve çok cihaz davranışı korunacak; yalnız birkaç satırı helper'a taşımak yeterli değil. Kesin server/local tamamlama politikası 27. maddede netleşecek.

### 8.3 Kabul edilen karar B — Ortak para doğrulama ve hesaplar

- Add/Detail/Budget tutar ayrıştırma/doğrulama kuralları ortak anlamda tutarlı olacak; ortak bütçe ve dönem hesapları tek sahibinde bulunacak.
- Home/Budget/AI aynı kapsam ve snapshot anlamında farklı hesap kopyaları kullanmayacak.
- Aylık ve tüm zamanlar gibi farklı kapsamlar tek sonuçmuş gibi birleştirilmeyecek; açık dönem girdileri/projection korunacak.
- Kesin para ölçeği, dönem sınırı ve bütçe kuralları 26. maddede; ortak saf hesap yaklaşımı önceki kararlarla uyumlu.

### 8.4 Kabul edilen karar C — Worker planlama/iptal sahipliği

- Fotoğraf yükleme ve hatırlatma work oluşturma, input, isim/tag ve iptal kuralları ilgili küçük sahiplerde yönetilecek.
- ReminderScheduler gibi gerçek alan sınırı kullanılabilir; fotoğraf/bildirim/sync'i tek genel WorkerManager içinde toplama yok.
- Account/record kimliği ve kalıcı iş uyumluluğu korunacak. Unique work/retry/dedupe ve medya sırası ilgili maddelerde ayrıca netleşecek.

### 8.5 Kabul edilen karar D — FirebaseSyncService tekrarlarının ayrıştırılması

- Tür mapper/local/remote veri işlemleri ilgili feature sahiplerine ayrılacak; ortak koordinasyon tür alanlarını bilmeyecek.
- Gerçekten aynı gönderme/sonuç davranışı küçük ortak yardımcılarla paylaşılabilecek; mevcut syncToFirebase değerlendirilip yararlı kısmı korunacak.
- Scheduled silmenin bildirim iptali ve AI mesaj sync sonucu gibi anlamlı farklar generic modele zorlanmayacak.
- Dosya satır sayısının azalması amaç değil sonuç; gerçek sorumluluk/tekrar ayrımı hedef. Genel adapter registry/plugin/BaseRepository ağı kurulmayacak.
- İptal, UID/generation ve kalıcı pending iş davranışı ilgili kararlarla birlikte ele alınacak.

### 8.6 Kabul edilen karar E — Gerçek ortak UI

- Auth form kabuğu/şifre alanı, işlem kategori/tutar alanları ve fotoğraf seçme içeriği gibi adaylar kullanım ve anlamına göre değerlendirilecek.
- Ortak UI görünümü paylaşacak; ekranların giriş/kayıt/kaydetme iş kurallarını içine toplamayacak. State/callback sınırı korunacak.
- Bütün ekranları onlarca boolean ile yöneten genel form veya kart framework'ü oluşturulmayacak.
- Ortak Edit* bileşenlerinin isimleri için 6. madde kullanıcı istisnası korunacak; paket taşıması isim değiştirme yetkisi değil.

### 8.7 Kabul edilen karar F — Merkezi kuralların ayrı sahipleri

- Aynı hata dönüşümü/kullanıcı mesajı 18, renk/şekil/gerçek ortak ölçü 19, dispatcher sağlama/scope politikaları 15–17. maddelerde kendi sınırlarında ortaklaştırılacak.
- Tek CommonManager veya büyük utils dosyası oluşturulmayacak; tüm coroutine işleri tek global scope'a taşınmayacak.
- Bu onay ilgili maddelerin henüz konuşulmamış ayrıntılarını otomatik onaylamaz.

### 8.8 Kabul edilen karar G — Ortaklaştırma ölçütü

- Aynı anlam/iş kuralı, aynı değişim nedeni, uyumlu hata/iptal/yaşam süresi ve çağrı noktalarında gerçek sadeleşme birlikte değerlendirilecek.
- İki kısa benzer blok soyutlama zorunluluğu doğurmaz. Ortak yapı fazla koşul veya parametre gerektiriyorsa ayrı tutmak uygun olabilir.
- Ortaklaştırma veri atomikliğini, hesap izolasyonunu veya kullanıcı davranışını bozmayacak; benzer görünen farklı yan etkiler gizlenmeyecek.

### 8.9 Kapsam ve tamamlanma ölçütü

- Etkilenecek alanlar: schedule completion ekran/receiver, ilgili transaction/schedule iş sınırı, Add/Detail/Budget validation, Home/Budget/AI hesaplar, medya/hatırlatma scheduler'ları, sync tür işlemleri ve gerçek ortak UI.
- Faz öncesi somut dosya planında neyin birleştirileceği/ne farkın korunacağı belirtilecek; yalnız dosya taşıma veya helper sayısı başarı ölçüsü değil.
- Nihai kontrolde kuralın tek sahibi, çağrıların aynı sözleşmeyi kullanması, hata/kimlik/iptal davranışı ve gereksiz generic koşulların bulunmaması değerlendirilecek.
- Kalıcı kimlikler/worker verisi/Firestore alanları ve kullanıcı istisnaları korunacak; ürün politikaları ilgili maddelerde ayrıca kararlaştırılacak.
- Tüm maddeler bitmeden refactor başlamayacak. İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur yalnız belgeler düzenlendi, test/build/lint çalıştırılmadı.

## Madde 9 — Büyük ekranlar ve çoklu sorumluluklar

Durum: Kullanıcı altı kararı kabul etti. Tekrar bulunmasa bile ekranın niyetini daha anlaşılır yapan anlamlı alt composable'lar çıkarılacak. Satır sayısı tek başına parçalama gerekçesi değildir. Üretim kodu değişmedi.

### 9.1 Kabul edilen karar A — Route ve içerik sınırı

- Route ViewModel bağlantısı, state toplama, navigation, permission ve launcher etkileşimlerini yönetecek.
- Ekran içeriği gereken state/callback'leri alıp UI oluşturacak; her küçük ekran için ayrı Route/Screen dosyası zorunlu değil.
- Lifecycle/state/navigation ayrıntıları ilgili maddelerle hizalanacak; alt bileşenlere ViewModel aktarılmayacak.

### 9.2 Kabul edilen karar B — Tekrar olmasa da anlamlı UI parçaları

- Kullanıcının GoogleSignInButton örneği genel okunabilirlik beklentisidir; yalnız Google butonuna sınırlı değildir.
- İkon/metin/loading ayrıntıları ana ekran akışını kaplıyorsa görevini anlatan alt composable çıkarılabilecek; form bölümleri, ekler, özetler, filtreler ve diyaloglar da bu ölçütle değerlendirilecek.
- GoogleSignInButton gibi auth'a ait parça içeride mevcut EditButton kullanabilir. Ortak bileşenin adını değiştirmekle ekranın anlamlı parçasını çıkarmak farklı kararlardır; Edit* isimlendirme istisnası korunacak.
- Her Row/Text veya birkaç satırlık basit görünüm için ayrı composable oluşturulmayacak.

### 9.3 Kabul edilen karar C — Somut ekran sorumlulukları

| Alan | Ayrım sınırı |
|---|---|
| Sign-in/sign-up | Form, anlamlı giriş eylemleri, verification dialog; launcher Route'ta |
| Add transaction | Form, fotoğraf/konum ekleri, tarih seçimi ve kaydetme eylemi |
| Transaction detail | Detay içerik, düzenleme sheet, fotoğraf görüntüleyici; launcher Route'ta |
| History | Filtre/list/kart; kart NavController yerine callback alır |
| Home | Finans özeti, öneri, grafik bölümleri |
| Location picker | İzin bağlantısı, arama/harita içerik; kamera animasyonu UI'da |

- Tablo zorunlu sınıf sayısı değil ayırma sınırıdır; ilgili fazda kesin dosya planı çıkarılacak.
- Parçalama mevcut form/draft, izin, feedback ve navigation davranışını kontrolsüz değiştirmeyecek.

### 9.4 Kabul edilen karar D — Mevcut iyi ayrımları koruma

- AiChatScreen mevcut MessageList/Bubble/Input gibi ayrımlarını koruyacak; gereksiz yeniden yazılmayacak.
- FilledBudgetContent mevcut card/list/helper ayrımlarını koruyacak; gerektiğinde ayrı dosyalar ve sade bağımlılıklarla okunabilirlik artırılacak.
- İnce BudgetScreen gereksiz katmanlarla büyütülmeyecek. Uzun dosya tek başına yanlış tasarım kanıtı değil.

### 9.5 Kabul edilen karar E — Fonksiyon/dosya/paket ayrımı

- Küçük ekran-yerel parça aynı dosyada private kalabilir; büyük/bağımsız anlamlı parça ekran yanında ayrı dosyada olabilir.
- Aynı feature içinde gerçek ortak parça presentation/components; feature'lar arası gerçek ortaklık core/ui/components için değerlendirilecek.
- Her alt composable ortak bileşen yapılmayacak; auth'a ait Google giriş butonu auth içinde kalacak.
- Görünürlük ve stil konvansiyonları 11. maddede, kesin parametre sınırları 10. maddede ayrıca ele alınacak.

### 9.6 Kabul edilen karar F — Gerçek sorumluluk ayrımı

- Alt bileşene tüm ViewModel veya gereksiz geniş state verilmeyecek; ihtiyaç kadar veri ve callback aktarılacak.
- İş kuralları, veri kaydetme/sync ve finansal hesap composable içinde olmayacak; ilgili iş/veri sahibinde bulunacak.
- Odak, menü ve animasyon gibi yerel UI davranışları uygun UI sahibinde kalabilecek; tüm state'i ViewModel'e taşıma zorunluluğu yok.
- Yalnız kodu başka dosyaya taşıyarak sorumluluk sorunu çözülmüş sayılmayacak.

### 9.7 Kapsam ve tamamlanma ölçütü

- Tüm ekranlar bu ölçütlerle değerlendirilecek; tablodaki örnekler kapsamı yalnız bu ekranlarla sınırlamaz.
- Ana ekran akışı okunabilir, parçalar açık görevli, değişiklik doğru bölümde yapılabilir olmalı; 100 satır gibi keyfi sınır kullanılmayacak.
- Önceki katman/ortaklık/isim kararları ve Edit* istisnası korunacak. Faz öncesi somut dosya planı çıkarılacak.
- Bütün maddeler bitmeden refactor başlamayacak. İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur yalnız karar belgeleri güncellendi, test/build/lint çalıştırılmadı.

## Madde 10 — Fazla parametreli sınıf ve fonksiyonlar

Durum: Kullanıcı beş kararı kabul etti. Parametre sayısı tek başına kalite/ihlal ölçüsü değildir; tek görev, ilişkili veri ve açık çağrı esas alınacak. Üretim kodu değişmedi.

### 10.1 Kabul edilen karar A — Constructor sorumlulukları

- Rapordaki FirebaseSyncService 8 ve AiRepositoryImpl 7 bağımlılık, farklı işleri birlikte yönetme bulgularıyla değerlendirilecek; yalnız sayıya göre parçalanmayacak.
- Önceki sorumluluk kararları uygulanarak bağımlılıklar doğru sahiplerine ayrılacak; Dependencies nesnesine doldurup sayıyı gizleme yok.

### 10.2 Kabul edilen karar B — Anlamlı veri grupları

- Birlikte doğrulanan tutar/kategori/tarih/not gibi işlem formu verileri TransactionDraft benzeri anlamlı modelde gruplanabilir; kesin model alanları ilgili fazda belirlenir.
- İlgisiz parametreleri aynı modele koyma veya her fonksiyon için Params sınıfı oluşturma yok.
- Gerekli yerlerde işlem başında tutarlı draft snapshot kullanılacak; await sonrası değişmiş form alanlarıyla kısmi işlem kurulmayacak.

### 10.3 Kabul edilen karar C — Dar composable veri/eylem yüzeyi

- Alt bileşen yalnız ihtiyaç duyduğu alan/callback'leri alacak; tüm ViewModel veya gereksiz geniş ekran state'i aktarılmayacak.
- İlişkili birçok alanı kullanan form bölümü küçük anlamlı state alabilir; tek butona tüm ekran state'i verilmez.
- Callback'ler otomatik büyük Actions nesnesine toplanmayacak; gerçek çağrı okunabilirliği değerlendirilecek.

### 10.4 Kabul edilen karar D — Davranış bayrakları

- isLogin/isEdit/isBudget/showPhoto gibi farklı senaryoları tek bileşene yükleyen bayrak kümeleri gerektiğinde anlamlı varyant veya ayrı bileşenle sadeleştirilecek.
- enabled/isLoading/isError gibi açık bağımsız durumlar sırf boolean oldukları için yanlış sayılmayacak.
- Geçersiz/çelişkili parametre kombinasyonları ve bileşenin farklı iş akışlarını üstlenmesi değerlendirilecek; genel form framework'ü kurulmayacak.

### 10.5 Kabul edilen karar E — Geçerli UI özelleştirme

- EditTextField'ın rapordaki 13 parametresi tek başına parçalama gerekçesi değil; modifier/klavye/ikon/görünüm gibi gerekli özelleştirmeler korunacak.
- Gereksiz/yinelenen/çelişkili parametreler gerçek kullanıma göre sadeleştirilecek; mevcut Edit* isimlendirme istisnası korunacak.
- En fazla beş parametre gibi keyfi limit yok; çağrı okunabilirliği, tek görev, ilişkili veri ve geçerli kombinasyonlar ölçüt.

### 10.6 Uygulama sınırı ve tamamlanma

- Tüm sınıf/fonksiyon/composable yüzeyleri bu ölçütlerle değerlendirilecek; örnekler kapsamı yalnız bu sınıflarla sınırlamaz.
- İmza değişikliğinde çağrı noktaları ve davranış korunacak; kalıcı alan/API değişimi yalnız parametre düzenlemesi bahanesiyle yapılmayacak.
- Faz öncesi somut dosya planı; tüm maddeler bitmeden refactor yok. İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur test/build/lint çalıştırılmadı.

## Madde 11 — Kod tutarlılığı

Durum: Kullanıcı yedi kararı kabul etti. by/.value yalnız örnektir; kapsam bütün uygulama ve bütün refactor önerileridir. Eşdeğer durumlarda tutarlı yaklaşım, farklı ihtiyaçta gerekçeli farklılık korunacak. Üretim kodu değişmedi.

### 11.1 Kabul edilen karar A — Kotlin yazım/dosya düzeni

- Girinti, satır kırılımı, import ve annotation yerleşimi tutarlı olacak.
- Kısa açık ifadede expression body; çok adımlı işte normal blok kullanılabilir. Her fonksiyonu tek satıra indirme yok; okunabilirlik öncelikli.

### 11.2 Kabul edilen karar B — Görünürlük ve API

- Yalnız dosya/sınıf içinde kullanılan yardımcılar private; dışarıya gerekli sözleşmeler açılacak. Gereksiz mutable alanlar kapatılacak.
- internal paket gizliliği değildir; tek Gradle modülüne açıktır. Otomatik internal ekleme yerine gerçek erişim gereksinimi değerlendirilecek.

### 11.3 Kabul edilen karar C — State sahipliği/erişimi

- Aynı state dışarıdan farklı yollarla değiştirilmeyecek; açık eylemler ve tek sahip esas alınacak.
- Compose'da yalnız güncel değer gereken yerde by, State nesnesi gereken yerde .value kullanılabilir; farklı kullanımlar tek başına hata değildir.
- State türü/toplama politikası 12–13. maddelerde; eşdeğer durumlar için tutarlı tercih uygulanacak.

### 11.4 Kabul edilen karar D — İşlem sonucu tutarlılığı

- Kaydet/sil/yükle için başarı/hata/loading/pending anlamları açık olacak; hata yutma ve ham exception gösterme gibi sebepsiz farklar giderilecek.
- Yerel kayıt başarısı uzak sync başarısı değildir. Her işlem tek generic Result veya dev event bus'a zorlanmayacak.
- Typed failure/UI mesajı ve durable işlem ayrıntıları ilgili maddelerde ayrıca netleşecek.

### 11.5 Kabul edilen karar E — Callback/fonksiyon kullanımı

- Basit imzası uyumlu yönlendirme için fonksiyon referansı; dönüşüm/ek UI davranışı için lambda kullanılabilecek.
- Kullanıcı eylemlerinde açık isim ve eşdeğer bağlantı tarzı; kesin viewModel:: kullanım kuralları 14. maddede.

### 11.6 Kabul edilen karar F — Coroutine/yan etki tutarlılığı

- Benzer işlerde dispatcher, iptal, hata ve tamamlanma yaklaşımı tutarlı olacak; beklenen/kontrolsüz başlatılan eşdeğer iş farkları incelenecek.
- UI, session ve durable background farklı yaşam sürelerine sahip olabilir; aynı global scope'a zorlanmayacak. Ayrıntılar 15–17. maddelerde.

### 11.7 Kabul edilen karar G — UI API ve kullanılmayan kod

- Composable'da uygun olduğunda modifier: Modifier = Modifier ilk optional parametre; doğru köke uygulanacak. Parametre/default anlamları tutarlı olacak.
- Kullanılmayan import/state/parametre ve gereksiz annotation gerçek kullanımı doğrulanarak temizlenecek.
- Resource gibi dinamik referanslı yapılar yalnız metin aramasında görünmediği için silinmeyecek; kullanıcı değişiklikleri korunacak.

### 11.8 Uygulama geneli ölçüt ve sınırlar

- Tutarlılık yalnız by/.value değil isim, state, callback, hata, coroutine, UI ve katman kullanımı dahil bütün kararlara uygulanacak.
- Benzer görevler sebepsiz farklı alışkanlıklarla yazılmayacak; farklı yaklaşımın gerçek sorumluluk/davranış gerekçesi olacak.
- Kullanıcının Edit* isimlendirme istisnası korunacak. Tutarlılık gerekçesiyle önceki istisnalar geçersiz sayılmayacak veya gereksiz ürün/API değişikliği yapılmayacak.
- Tüm maddeler bitmeden refactor yok; sonunda uygulama genelinde bütün başlıklar tekrar kontrol edilecek. İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur test/build/lint çalıştırılmadı.

## Madde 12 — UI state, stateIn ve WhileSubscribed

Durum: Kullanıcı altı kararı kabul etti. Mevcut doğru kullanımlar korunacak; bütün state'lere mekanik operatör eklenmeyecek. Üretim kodu değişmedi.

### 12.1 Kabul edilen karar A — Açık state sahibi/kaynağı

- ViewModel state'i salt okunur açılır; UI değişiklikleri named actions üzerinden iletilir.
- Birlikte değişmesi gereken alanlar anlamlı UiState içinde; Budget ekran/form/delete dialog gibi bağımsız state'ler zorla tek nesneye yığılmaz.
- Aynı bilgi için tutarsız paralel kaynak oluşturulmaz; form/draft ve kaydetme snapshot'ı önceki kararlarla uyumlu.

### 12.2 Kabul edilen karar B — Veri akışından UI state üretimi

- Room/repository Flow'undan ekran state'i üretirken genel tercih stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), anlamlı initialState).
- Akışın gerçek yaşam süresi değerlendirilir; uygun mevcut kullanımlar korunur, her Flow'a aynı politika zorunlu değil.
- Timeout eşdeğer durumlarda tutarlı adlandırma/yazımla kullanılacak; bunun için gereksiz genel framework kurulmaz.

### 12.3 Kabul edilen karar C — Doğrudan mutable state

- ViewModel'in doğrudan yönettiği state için private MutableStateFlow + asStateFlow yeterli olabilir; yeniden stateIn ile sarılmaz.
- Tutarlılık bütün state türlerini aynı operatör zincirine zorlama anlamına gelmez. Yerel Compose state uygun UI sahibinde kalabilir.

### 12.4 Kabul edilen karar D — Anlamlı ilk değer ve durumlar

- emptyList/null ilk değerinin loading/gerçek boş/bulunamadı durumlarını karıştırması giderilecek.
- Liste için loading/empty/error/content, detay için loading/not-found/error/content anlamları açık olacak; her ekran aynı dev generic state modeline zorlanmayacak.
- Veri korunurken yenileme/kaydetme durumunun gösterilmesi gerekiyorsa anlamlı ayrı alan kullanılabilir; gereksiz içerik kaybı yaratılmayacak.

### 12.5 Kabul edilen karar E — Filtre ve ilişkili akışlar

- History sayısal filterTrigger yerine anlamlı filtre state'i üzerinden akış kuracak.
- Home tekrar eden gelir/gider akışları sadeleştirilecek; combine tek başına atomik finans snapshot garantisi sayılmayacak, gerektiğinde data katmanında tutarlı okuma sağlanacak.
- Gereksiz yinelenen stateIn/distinctUntilChanged/flowOn temizlenecek; gerekli kullanım korunacak.

### 12.6 Kabul edilen karar F — UI aboneliği ve iş yaşam süresi

- WhileSubscribed(5_000) son gözlemci ayrıldıktan sonra upstream durdurma için bekler; ViewModel'i veya bütün işleri iptal etmez, varsayılan son değeri hemen sıfırlamaz.
- Durable sync/pending kayıt gönderimi/session yönetimi ekranın açık olmasına bağlanmayacak; ayrı doğru sahip/kalıcı iş mekanizması korunacak.
- Hesap değişiminde eski kullanıcı state'i açık biçimde temizlenecek/ayrılacak; WhileSubscribed cache davranışı hesap izolasyonunun yerine geçmez.
- Menüler/odak gibi yerel UI state uygun yerde kalır. Process-death için gereken filtre/draft SavedStateHandle/rememberSaveable ile ihtiyaca göre değerlendirilir; hassas/büyük veri körlemesine saklanmaz.

### 12.7 Kapsam ve tamamlanma sınırı

- Tüm ilgili ViewModel/ekran state'leri değerlendirilecek; mevcut doğru Auth/Budget asStateFlow ve çeşitli WhileSubscribed kullanımları gereksiz yeniden yazılmayacak.
- Flow üretimi, state mutation, loading/error ve hesap sahipliği ayrımı kontrol edilecek; lifecycle-aware toplama 13. maddede ayrıca kararlaştırılacak.
- Edit* istisnası ve uygulama geneli tutarlılık korunur. Bütün maddeler bitmeden refactor yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacak. Bu tur test/build/lint çalıştırılmadı.

## Madde 13 — Lifecycle-aware UI toplama

Durum: Kullanıcı beş kararı ve açıklanan repeatOnLifecycle sınırını kabul etti. Auth/Home/Budget/History/Detail/Schedule/AI/location ekranlarında collectAsStateWithLifecycle zaten kullanılıyor; olmayan eksiklik üretilmeyecek. Üretim kodu değişmedi.

### 13.1 Kabul edilen karar A — Mevcut doğru UI toplama

- Android Compose ekranının gösterdiği Flow/StateFlow için genel tercih collectAsStateWithLifecycle; doğru mevcut kullanımlar korunacak.
- Doğrudan Compose State sırf tutarlılık için Flow'a çevrilip yeniden toplanmayacak.

### 13.2 Kabul edilen karar B — Route sınırında toplama

- Genel olarak state Route'ta toplanıp içerik/alt bileşene aktarılacak; aynı akış alt bileşenlerde gereksiz tekrar toplanmayacak, ViewModel aktarılmayacak.
- Gerçek bağımsız UI bölümü için farklı toplama ihtiyacı gerekçesiyle değerlendirilebilir; tek collector keyfi zorunluluk değil.

### 13.3 Kabul edilen karar C — Manuel collector yaşam süresi

- UI'a bağlı manuel collect gerektiğinde repeatOnLifecycle gibi uygun lifecycle mekanizması değerlendirilecek; normal state gösteriminde collectAsStateWithLifecycle yeterli, üstüne ek repeatOnLifecycle yok.
- LaunchedEffect composition çıkışı/key değişimiyle iptal edilir; Activity arka plana geçti diye otomatik durduğu varsayılmayacak. Her LaunchedEffect yanlış değil; animasyon/odak/UI etkisinin gerçek ihtiyacı korunacak.
- repeatOnLifecycle(STARTED) bloğu lifecycle STARTED/RESUMED durumunda çalışır, eşik altına düşünce iptal, yeniden aktif olunca yeniden başlar; kaldığı satırdan devam değildir.
- Yeniden başlayan bu blok içine kayıt/sync mutasyonu konup tekrar işlem tetiklenmeyecek.
- Merkezi dispatcher thread seçimi, scope/job yaşam süresi ve lifecycle UI gözlemi ayrı kavramlardır; merkezi dispatcher lifecycle toplamanın yerine geçmez. Bütün işler tek ortak scope'a toplanmayacak.

### 13.4 Kabul edilen karar D — UI sonucu tekrar/kayıp davranışı

- Lifecycle-aware toplama navigation/Snackbar'ın yalnız bir kere işlenmesini kendiliğinden garanti etmez.
- Auth sonuç/reset gibi akışlarda hangi sonucun işlendiği/tüketildiği açık olacak; kritik kayıt başarısı yalnız kaybolabilecek geçici event'e bağlı olmayacak.
- Her ekran için genel event bus veya zorunlu Channel kurulmayacak; sonuç/state ve UI sahipliği ilgili kararlarla hizalanacak.

### 13.5 Kabul edilen karar E — Background işi ayırma

- UI state toplamasının durması kayıt/sync/hatırlatma işinin otomatik iptali sayılmayacak; doğru iş sahibi ve durable mekanizma kullanılacak.
- Gerekli lifecycle Compose dependency doğrudan/tutarlı tanımlanacak; sürüm düzenlemesi 32. maddede.

### 13.6 Kapsam ve tamamlanma ölçütü

- Tüm Route/ekran state collection ve UI effect noktaları değerlendirilir; mevcut doğru kullanımlar korunur, gereksiz lifecycle sarmalaması eklenmez.
- UI aktiflik ihtiyacı, collector sahipliği ve kritik sonuç tüketimi açık olmalı; şirketlerde tek evrensel uygulama biçimi iddia edilmez, resmî rehbere uygun ihtiyaca göre kullanım hedeflenir.
- Edit* istisnası ve uygulama geneli tutarlılık geçerli. Tüm maddeler bitmeden refactor yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacak. Bu tur test/build/lint çalıştırılmadı.

## Madde 14 — Fonksiyon referansları ve lambda tercihleri

Durum: Kullanıcı beş kararı kabul etti. viewModel::login yalnız örnektir; tüm uygulama callback bağlantıları bu ölçütlerle değerlendirilecek. Üretim kodu değişmedi.

### 14.1 Kabul edilen karar A — Basit uyumlu yönlendirme

- Callback yalnız aynı parametrelerle bir fonksiyonu çağırıyorsa ve imzalar uyumluysa fonksiyon referansı tercih edilir; örneğin onValueChange = viewModel::updateEmail.
- Mevcut SignInScreen updateEmail/updatePassword forwarding örnekleri bu kapsamda. Amaç açık bağlantı ve gereksiz sarmalamayı azaltmak, mekanik tüm lambda dönüşümü değil.

### 14.2 Kabul edilen karar B — Lambda gerektiren adaptasyon

- Argüman bağlama/dönüştürme veya ek UI davranışında lambda korunur; örneğin onClick = { onTransactionClick(transaction.id) }.
- Referans adına gereksiz wrapper metot veya ayrı sınıf oluşturulmaz; overload/default/return imza uyumu gerçek çağrı bağlamında kontrol edilir.

### 14.3 Kabul edilen karar C — Anlamlı eylem sahipliği

- UI callback içinde doğrulama/kaydetme/form temizleme iş akışı kurulması yerine ilgili ViewModel/iş sahibinin açık eylemi kullanılır.
- AI onSendClicked gibi eylem gönderme guard'ı ve input temizleme davranışını uygun sahipte yönetir; yalnız onClick'in içinde birden fazla VM çağrısı ile iş sırası kurulmaz.
- Klavye kapatma, launcher açma ve navigation uygun UI sınırında kalır; referans tercih etmek bu UI işlerini ViewModel'e taşıma gerekçesi değildir.

### 14.4 Kabul edilen karar D — Alt UI callback sözleşmesi

- Alt bileşen ViewModel değil ihtiyaç duyduğu state ve callback'leri alır. TransactionForm onAmountChanged/onSaveClick gibi eylemlerle çalışır.
- Callback referansı Route'ta ViewModel'e bağlanabilir; alt UI bunun kaynağını bilmez. Yalnız auth/login değil bütün ilgili bileşenler kapsanır.

### 14.5 Kabul edilen karar E — Tutarlılık, performans sınırı

- Basit uyumlu yönlendirme → fonksiyon referansı; adaptasyon/gerçek UI davranışı → lambda. Eşdeğer durumlarda uygulama geneli tutarlılık korunur.
- :: tek başına daha hızlı çalışma/az recomposition garantisi değildir; her callback sırf bunun için remember ile sarılmaz.
- Callback değişimi eski veri yakalama veya işlem sırası farkı yaratmayacak; imza/çağrı noktası ve yaşam süresi değerlendirilecek.

### 14.6 Kapsam ve uygulama sınırı

- Tüm ilgili Route/Screen/alt bileşen callback'leri değerlendirilecek; dönüşüm okunabilirliği artırmalı, named actions state sahipliğini korumalı.
- Önceki Edit* istisnası ve katman/UI sahipliği korunur. Tüm maddeler bitmeden refactor yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacak. Bu tur test/build/lint çalıştırılmadı.

## Madde 15 — Coroutine, scope ve cancellation

Durum: Kullanıcı altı kararı ve GuideMate karşılaştırması sonrası merkezi tanımlar/işe özel yaşam süresi açıklamasını kabul etti. Üretim kodu değişmedi. SupervisorJob/ApplicationScope ayrıntıları 16, dispatcher ayrıntıları 17. maddede netleştirilecek.

### 15.1 Kabul edilen karar A — Doğru yaşam süresi

- Ekran state'i/arama/talep viewModelScope; sheet/scroll/harita animasyonu uygun UI scope'u; hesap sync listener/koordinasyonu iptal edilebilir session işi; kalıcı sync/upload/reminder DB niyeti + WorkManager sahibi olacak.
- Bütün işler tek merkezi scope'a taşınmayacak; işin sahibi, süresi ve iptal davranışı açık olacak.

### 15.2 Kabul edilen karar B — Açık tamamlanma

- Tek seferlik veri işlemi genel olarak suspend, veri gözlemi Flow sunacak; repository kontrolsüz launch başlatıp işi bitmiş saymayacak.
- Logout DB temizliği yeni login ile yarışmayacak; gerekli sıralama/tamamlanma oturum sahibinde yönetilecek. Veri silme/koruma politikası 24. maddede ayrıca belirlenir.

### 15.3 Kabul edilen karar C — İptali hata olarak yutmama

- catch(Exception) ve runCatching gibi geniş yakalama noktalarında CancellationException normal kullanıcı/ağ hatasına çevrilmeyecek; rethrow edilecek.
- Kaynak kapanışı/gerekli cleanup finally ile yönetilecek; suspend cleanup iptal davranışı gerektiğinde ayrıca tasarlanacak, genel NonCancellable sarmalama yok.
- Hata dönüşümü ve log hassas veri sızdırmadan açık olacak; ayrıntılar 18. maddede.

### 15.4 Kabul edilen karar D — Eski/tekrarlı işi kontrol

- Son isteğin geçerli olduğu arama/konum işlerinde önceki job gerektiğinde iptal edilir; kaydet/tamamla çift tetiklemesi korunur.
- Hesap değişiminde eski listener/iş durdurulur; geç gelen sonuç UID/generation kontrolüyle reddedilir.
- Coroutine iptali uzak mutasyonu geri almaz; finans işlemlerinde atomiklik/idempotence gerekir. Her kaydetmeye latest-cancel uygulanmaz.

### 15.5 Kabul edilen karar E — SDK sonucunu bekleme

- Sonucu gereken Firebase Task çağrıları uygun await ile beklenir; başlatmak başarı sayılmaz.
- Coroutine iptali her SDK/server isteğini durdurmaz; sonuç/pending/retry davranışı bunu dikkate alır.

### 15.6 Kabul edilen karar F — Service/receiver girişleri

- FCM service ve receiver kısa giriş noktaları; uzun/dayanıklı işler uygun kalıcı mekanizmaya devredilir.
- Kısa bounded receiver işinde gerektiğinde goAsync + finally finish; sınırsız süre/process survival garantisi değil.
- Gerçek service-owned coroutine kalırsa servis kapanışında iptal edilir; bağımsız sınırsız scope ile işin yaşayacağı varsayılmaz.

### 15.7 Kullanıcıyla netleştirilen merkezi yapı ve GuideMate karşılaştırması

- Coroutine altyapı tanımları merkezi DI modülünden sağlanacak: gereken dispatcher'lar, somut process-lifetime ihtiyaç varsa ApplicationScope.
- Merkezi tanım bütün işleri applicationScope.launch içinde çalıştırmak değildir. viewModelScope korunur; session job'ları hesap sahibinde iptal edilir/yeni girişte yeniden oluşturulur; durable işler WorkManager'dadır.
- GuideMate CoroutineModule güncel kaynağı read-only incelendi: Singleton ApplicationScope, SupervisorJob + Dispatchers.IO sağlar; chat/notification kullanımları vardır.
- ChatRealtimeSessionManager job'ları stop ile iptal edip session user/generation kontrolü yapar. Örnek alınan ilke yalnız scope paylaşımı değil iş sahipliği/iptal/eskimiş sonuç reddidir; GuideMate'in tüm coroutine davranışı doğrulanmış sayılmaz.
- FinanceAI bu DI/qualifier ilkesini ihtiyaca göre kullanacak; dispatcher enjekte edilecek, genel CoroutineManager kurulmayacak. ApplicationScope süreç ölünce kaybolur, kalıcı iş garantisi değildir.
- Hesaba bağlı job ile ApplicationScope arasındaki kesin bağ/organizasyon 16. maddede; GuideMate birebir kod kopyası veya o projede değişiklik yetkisi yok.

### 15.8 Kapsam ve tamamlanma

- Sync/auth/AI/media/location/worker/service/receiver ve ilgili ViewModel coroutine noktaları sahiplik/iptal/tamamlanma açısından değerlendirilecek.
- Merkezi kurallar ile farklı iş yaşam süreleri ayrı kalacak; eski hesap sonucu ve pending işlem güvenliği korunacak.
- Tüm maddeler bitmeden refactor yok; Edit* istisnası ve genel tutarlılık korunur. İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur test/build/lint çalıştırılmadı.

## Madde 16 — SupervisorJob ve ApplicationScope

Durum: Kullanıcı beş kararı kabul etti. Merkezi coroutine tanımları ile farklı iş yaşam süreleri birlikte korunacak; GuideMate yaklaşımı ilke olarak örnek, birebir kopya değil. Üretim kodu değişmedi.

### 16.1 Kabul edilen karar A — Gereken process-lifetime scope

- Ekrandan bağımsız, süreç boyunca gereken hesap değişimi gözlemcisi gibi somut koordinasyon işi varsa Hilt üzerinden tek qualifier'lı ApplicationScope sağlanacak.
- Mevcut bağımsız scope'ları sırf toplamak için bütün işleri buraya taşıma yok; süreç ömrü gerçek ihtiyaca dayanacak.

### 16.2 Kabul edilen karar B — Bağımsız hata izolasyonu

- Aynı scope'taki bağımsız kardeş işlerden birinin hatası diğerlerini iptal etmemeliyse SupervisorJob kullanılacak.
- SupervisorJob hata yönetimi veya crash önleme garantisi değildir; launch/async sonuçları uygun şekilde handle/await/log edilecek, iptal normal hata sayılmayacak.
- Birlikte tamamlanması gereken command adımları bağımsızlaştırılmayacak; structured coroutine ve DB transaction farklı garantilerdir.

### 16.3 Kabul edilen karar C — Hesap işi sahipliği

- ApplicationScope hesap gözlemcisi süreç boyunca yaşayabilir; aktif hesabın listener/job'ları oturum sahibinde ayrı iptal edilebilir sahiplikle yönetilecek.
- Logout ilgili hesap işlerini durduracak; uygulama scope'unun tamamı iptal edilmeyecek. Yeni login yeni session job oluşturacak; cancelled job yeniden kullanılmayacak.
- Geç sonuç UID/generation kontrolüyle reddedilecek; gerekli stop/await sırası açık olacak. Parent-child bağları uygun korunacak, farkında olmadan koparılmayacak.

### 16.4 Kabul edilen karar D — ViewModel ve iş devri

- viewModelScope korunacak; ViewModel'e ek SupervisorJob/global scope oluşturulmayacak.
- Aynı iş iki scope'ta tekrar başlatılmayacak. ViewModel talebi kendi scope'unda iletebilir, ekranı aşan iş uygun koordinatör sahipliğinde devam edebilir; talep/iş sahipliği ayrıdır.
- Dış scope işi ile caller bekleme/iptal ve sonuç sözleşmesi açık olacak; her repository metodu uygulama scope'una yönlendirilmeyecek.

### 16.5 Kabul edilen karar E — Dayanıklılık sınırı

- Süreç kapanınca ApplicationScope işleri kaybolur. Kalıcı sync/upload/reminder/gerekli retry için DB niyeti + WorkManager kullanılacak.
- Service/receiver'a scope injection tek başına süreci canlı tutmaz; kısa callback/devir ve ilgili lifecycle cleanup önceki maddeyle hizalanacak.
- Genel CoroutineManager yok; gereken merkezi scope/dispatcher tanımı, session işleri ve viewModelScope birlikte kullanılır.

### 16.6 Kapsam ve uygulama sınırı

- Merkezi DI/qualifier, session koordinasyonu, mevcut bağımsız auth/sync/service/receiver scope'ları ve çağrı noktaları ilgili faz planında ele alınacak.
- Scope yerleşimi 7. madde, dispatcher injection 17. madde ile hizalanacak. Logout veri politikası ayrı onaylanır; scope iptali veri silme yetkisi değildir.
- Tüm maddeler bitmeden refactor yok; Edit* istisnası ve genel tutarlılık korunur. İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur test/build/lint çalıştırılmadı.

## Madde 17 — Dispatcher yönetimi

Durum: Kullanıcı beş kararı ve sırf kullanmak için dispatcher eklememe ilkesini kabul etti. Merkezi dispatcher işin nerede çalışacağını sağlar; scope yaşam süresini belirler. Üretim kodu değişmedi.

### 17.1 Kabul edilen karar A — Merkezi DI sağlayıcıları

- Mevcut IoDispatcher yaklaşımı korunup tutarlı kullanılır. Gereken sınıf qualifier'lı CoroutineDispatcher alır.
- Gerçek CPU ihtiyacında DefaultDispatcher; Main binding yalnız somut ihtiyaçta. Bütün dispatcher'ları içeren nesne her sınıfa verilmez.
- Merkezi tanım kullanım tutarlılığı ve ileride kontrollü test dispatcher'ı sağlama içindir; işin ekranı aşmasını/process survival sağlamaz.

### 17.2 Kabul edilen karar B — İşe göre seçim

- Blocking dosya IO/Geocoder IO; ağır hesap/rapor/görsel işleme niteliğine göre Default, dosya kısmı IO; UI/hafif state Main.
- Küçük mapping/kimlik üretimi otomatik geçiş gerektirmez. Uzun veya suspend olması tek başına IO gerekçesi değildir.

### 17.3 Kabul edilen karar C — Main-safe iş sahibi

- Blocking işi yapan data source/adapter uygun dispatcher'a geçer; ViewModel repository çağrısının thread ayrıntısını yönetmek zorunda kalmaz.
- PhotoLocalStore gibi gerçek dosya sınırında injected IO + withContext değerlendirilecek; küçük forwarding metotlara gereksiz geçiş eklenmez.

### 17.4 Kabul edilen karar D — Zaten asenkron API

- Room suspend DAO/Flow sırf DB kullanılıyor diye ek withContext(IO) ile sarılmaz; yanındaki blocking iş ayrıca değerlendirilir.
- Uygun Firebase await beklerken thread bloke etmez; bitişik dosya/ağır hesap farklı sorumluluktur. Tüm ağ çağrısı otomatik IO wrapper gerektirmez.

### 17.5 Kabul edilen karar E — Gereksiz geçiş/sabit temizliği

- İş katmanındaki dağınık IO/Default seçimleri uygun injection ile düzenlenir; iç içe gereksiz withContext/yinelenen flowOn azaltılır.
- viewModelScope varsayılan Main davranışı korunur. IO ApplicationScope içindeki CPU işinin otomatik IO'da kalması doğru sayılmaz.
- Genel thread manager yok; küçük merkezi tanımlar ve gerçek işi yapan sınıfta doğru seçim. Job/scope parent ilişkisi dispatcher değişimi bahanesiyle koparılmaz.

### 17.6 Genel ilke ve tamamlanma ölçütü

- Kullanıcı ihtiyaç ilkesi bütün maddelere uygulanacak: yapı ancak gerçek fayda varsa eklenir; doğru mevcut kod korunur, keyfi soyutlama/katman/dispatcher yok.
- İlgili dosya/bitmap/geocoding/rapor, sync/auth/worker ve DI noktalarında blocking/CPU/asenkron ayrımı incelenecek; UI thread'i ağır işle bloke edilmeyecek.
- Kesin provider yerleşimi önceki paket/DI kararlarıyla hizalanacak; test edilebilirlik gelecekteki test başarısı iddiası değildir.
- Tüm maddeler bitmeden refactor yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacak. Edit* istisnası geçerli; bu tur yalnız karar belgeleri değişti, test/build/lint çalıştırılmadı.

## Madde 18 — Hata yönetimi ve XML mesajları

Durum: Kullanıcı yedi karar ve ortak/feature hata paketleri yerleşimini kabul etti. Merkezi yönetim aynı dönüşüm/mesaj kuralını tekrar yazmamak anlamındadır; dev ErrorManager veya tüm ekranları yöneten global Snackbar değil. Üretim kodu değişmedi.

### 18.1 Mevcut durum

- Raporda yinelenen tutar/kategori metinleri, ham exception mesajları, bütçe silmede transaction metni ve konum kaldırmada photo metni gibi uyumsuzluklar var.
- Data→presentation AuthException bağımlılığı önceki kararlarla kaldırılacak; hata türü, kaynak metni ve gösterim sahibi ayrılacak.

### 18.2 Kabul edilen karar A — SDK'dan uygulama hatasına

- Akış Firebase/SDK exception → anlamlı SDK'sız uygulama failure → presentation kaynak eşlemesi → strings.xml kullanıcı mesajı olacak.
- Data uygun sınırda teknik hatayı dönüştürür; domain Firebase exception/Android resource ID bilmez.

### 18.3 Kabul edilen karar B — Ortak/feature hatası sahipliği

- Gerçek ortak bağlantı/timeout/servis dönüşümleri paylaşılır; yanlış giriş/expired reset link/bütçe kuralı feature sahipliğinde kalır.
- Tek genel sınıfta bütün hatalar veya generic hata framework'ü kurulmaz; anlamlı minimal failure türleri kullanılır.

### 18.4 Kabul edilen karar C — Bağlantı nedenini doğru ifade

- Her Firebase hatasına internet yok denmez; yetki/timeout/servis/bağlantı ayrımı korunur.
- Kesin neden bilinmiyorsa bağlantı kurulamadı gibi sınırı doğru mesaj kullanılır; preflight internet kontrolü işlem başarısı garantisi değildir.

### 18.5 Kabul edilen karar D — Offline başarı/pending/failure

- Yerel kayıt başarılı, remote pending ise kaydetme başarısız gösterilmez; gerektiğinde kaydedildi/bağlantıda senkronize edilecek durumu kullanılır.
- Pending kayıt takip edilir; müdahale gerektiren kalıcı sync sorunu sessiz gizlenmez. Her geçici kesintide Snackbar üretilmez.
- Retry/durable sonuç ayrıntıları ilgili sync maddeleriyle hizalanır; kör finans mutasyonu tekrarından kaçınılır.

### 18.6 Kabul edilen karar E — XML kullanıcı metinleri

- Statik kullanıcı metni/hata/eylem etiketi XML; aynı anlamda yinelenen kaynak birleştirilir, farklı anlam korunur.
- snake_case kaynak isimleri; strings.xml bölünmesi zorunlu değil, gerçek okunabilirlik ihtiyacında feature ayrımı yapılabilir.
- Kullanıcı verisi ve AI üretimi XML'e taşınmaz; format argümanları anlamlı/güvenli, ham exception argümanı yok.

### 18.7 Kabul edilen karar F — Uygun gösterim

- Alan doğrulaması alan altında, yükleme hatası ekran state/retry, işlem feedback uygun Snackbar/içerik, kullanıcı kararı gereken yerde dialog.
- Aynı hata Toast/Snackbar/dialog olarak tekrar gösterilmez. Gösterim yeri ilgili ekranın sorumluluğu; ortak mapper bunu yönetmez.
- Sonucun tüketimi/tekrar davranışı 13. maddeyle hizalanacak.

### 18.8 Kabul edilen karar G — Teknik ayrıntı/iptal/retry

- Ham exception.message kullanıcıya çıkmaz veya normal AI cevabı olarak saklanmaz.
- Teknik log hassas veriyi sızdırmadan izlenebilir olur; email/token/finans içerikleri basılmaz.
- CancellationException normal kullanıcı hatası değil; geçici/kalıcı hata ayrılır, idempotence olmadan mutasyon kör tekrar edilmez.

### 18.9 Kabul edilen paket sahipliği

```text
core/error/                         ortak SDK'sız failure türleri
core/firebase/error/                gerçek ortak Firebase hata dönüşümleri
core/ui/error/                      ortak failure → XML eşlemesi
feature/auth/domain/error/          auth'a özgü failure türleri
feature/auth/data/mapper/           auth SDK hata dönüşümü
feature/auth/presentation/mapper/   auth failure → XML eşlemesi
```

- Auth örneği diğer feature'larda yalnız gerçek ihtiyaçta uygulanır; bütün feature'lara boş error/mapper paketleri açılmaz.
- Bağlantı gibi ortak anlam core, expired parola bağlantısı gibi anlam auth'ta; Firebase dönüşümü Android/SDK bilirken domain failure SDK'sız kalır.
- Ortak UI mapper yalnız ortak türleri; feature presentation mapper kendi türünü eşler ve gerektiğinde ortak eşlemeyi kullanır. Core feature hata türlerine bağımlı hale getirilmez.
- Paket/dosya yalnız yeterli gerçek içerikte açılır. Kesin dosya adları faz planında; merkezi yönetim tüm hataları tek yere veya tüm UI'yı global manager'a toplamak değildir.

### 18.10 Kapsam ve tamamlanma

- İlgili repository/SDK/worker/VM failure dönüşümleri, XML kaynakları, UI mapper ve ekran feedback noktaları birlikte değerlendirilir.
- Katman yönü, aynı anlamda mesaj tutarlılığı, doğru offline sonuç ve hassas log sınırı kontrol edilecek; bilinmeyen hata için güvenli fallback korunacak, her hata generic mesajla kaybedilmeyecek.
- Tüm maddeler bitmeden refactor yok; Edit* istisnası/genel tutarlılık geçerli. İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur test/build/lint çalıştırılmadı.

## Madde 19 — Hardcoded metin, renk ve ölçüler

Durum: Kullanıcı altı karar ve core/ui/theme/Spacing.kt yerleşimini kabul etti. Ortak tasarım değerleri merkezi; her literal ortaklaştırılmaz. Üretim kodu değişmedi.

### 19.1 Kabul edilen karar A — Statik kullanıcı metni

- Başlık/açıklama/buton/öneri metinleri XML'den gelir; VM gerektiğinde öneri türü ve parametre sağlar, presentation kaynak seçer.
- Kullanıcı notu/AI cevabı/Firestore alanı/teknik anahtar XML kullanıcı metni değil. AI sistem talimatı UI metninden ayrı sahiplikte.

### 19.2 Kabul edilen karar B — Semantic renk rolleri

- Ana kullanım MaterialTheme.colorScheme; yinelenen hex uygun rol altında ortaklaştırılır. onSurface/onPrimary yüzey anlamına göre kullanılır.
- Material rollerine sığmayan gerçek ürün rolü için küçük ek tanım mümkün; tüm ColorScheme ikinci sınıfta kopyalanmaz.

### 19.3 Kabul edilen karar C — Gradient ve durum renkleri

- Aynı anlamdaki Home/Budget summary gradient tek kaynaktan; farklı auth rolü ayrı kalabilir.
- Warning/success/error anlamları tutarlı; durum yalnız renkle anlatılmaz. Otomatik görsel yeniden tasarım yok.

### 19.4 Kabul edilen karar D — Küçük ortak spacing

- core/ui/theme altında Color.kt, Theme.kt, Type.kt ve gereken ortak aralıklar için Spacing.kt bulunacak; şekil/ek tema dosyaları gerçek içerikte.
- screenPadding/sectionGap gibi gerçek ortak roller merkezi; aynı sayının aynı anlamı taşıdığı varsayılmaz. Her 16.dp tek sabite bağlanmaz.
- 0.dp/yerel hizalama/çizim oranları yerel kalabilir; ekrana özgü genişlik/yükseklik/layout ölçüleri Spacing.kt'ye doldurulmaz.
- Yerleşim bu proje için profesyonel, uygun sahiplik tercihidir; tüm şirketler için tek zorunlu yol iddiası yok. Designsystem/tokens alternatifi büyüme gerekmeden eklenmez.
- Sabit küçük yapı yeterli olabilir; yalnız gerçekten değişken tema/layout ihtiyacında ek mekanizma, zorunlu CompositionLocal/framework yok.

### 19.5 Kabul edilen karar E — Typography/shapes

- Benzer metinlerde MaterialTheme.typography, ortak şekillerde MaterialTheme.shapes tercih edilir.
- Gerçek ayrı card/sheet rolü varsa küçük ek tanım; her radius/font size için global isim yok.

### 19.6 Kabul edilen karar F — Yerleşimi doğru düzeltme

- Reset ekranı büyük sabit üst boşluk/buton genişliği sadece constant'a taşınarak çözülmüş sayılmaz; gerekli hizalama/scroll/insets/genişlik sınırı düzeltilir.
- XML window/startup tema ile Compose tema tutarlı olacak. Dark/light/dynamic davranış bilinçli belirlenir; mevcut koyu görünüm kullanıcı kararı olmadan farklı tasarıma dönüştürülmez.
- Grafik paleti/yazısı kendi sahibi veya gerçek ortak tema rolünde; font scale/density ve durum erişilebilirliği ilgili maddelerle hizalanır.

### 19.7 Kapsam ve tamamlanma

- Home metinleri, yinelenen alan renkleri/gradient'ler, budget durum renkleri, gerçek ortak spacing, typography/shapes ve XML/Compose yüzeyleri kontrol edilir.
- Küçük anlamlı tema yapısı hedef; büyük design-system framework yok. Feature yerel ölçüleri/brand çizimleri/wire format gereksiz taşınmaz.
- Tüm maddeler bitmeden refactor yok; Edit* istisnası/genel tutarlılık geçerli. İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur test/build/lint çalıştırılmadı.

## Madde 20 — Type-safe navigation ve back stack

Durum: Kullanıcı altı kararı ve GuideMate'tekiyle aynı type-safe Navigation Compose API yaklaşımını kabul etti. Navigation Compose korunacak; Navigation3'e geçiş gerekmiyor. Üretim kodu değişmedi.

### 20.1 Kabul edilen karar A — Typed destination

- Argümansız hedef Serializable object, argümanlı hedef Serializable data class olacak; örnek HomeDestination/TransactionDetailDestination(transactionId: Int).
- navigate typed instance, graph composable<T>, selection hasRoute<T> ile kurulacak. Destination adı Route composable'ıyla karışmayacak; mevcut string enum ikinci paralel kaynak olarak kalmayacak.

### 20.2 Kabul edilen karar B — Typed argüman okuma

- Entry veya SavedStateHandle.toRoute<T>() ile okunacak; dağınık transactionId magic string ve sessiz 0 fallback kaldırılacak.
- Mevcut Int kimlik sırf navigation geçişi için Long/String'e çevrilmeyecek. Typed ID veri varlığı/yetki garantisi değil; NotFound/Error korunacak.

### 20.3 Kabul edilen karar C — Minimum argüman

- Detaya gerekli kayıt kimliği, güncel veri repository'den. Room entity/VM/büyük liste/access token route argümanı değil.
- Gerçek küçük UI bilgisi ihtiyaca göre; bütün ekran verisi route'a doldurulmayacak.

### 20.4 Kabul edilen karar D — Navigation sahipliği

- Kart/alt UI NavController/string route bilmez, onTransactionClick(id) gibi callback sunar; Route/graph sahibi navigation bağlar.
- Feature destination ilgili feature navigation sahipliğinde, app graph/shell app/navigation'da olacak; core feature hedeflerine bağımlı olmayacak.
- GuideMate güncel kaynağında Serializable destination/toRoute/hasRoute doğrulandı; aynı API prensibi örnek alınacak, guide/tourist graph'ları FinanceAI'ye kopyalanmayacak. FinanceAI auth/ana akışı için küçük yapı.

### 20.5 Kabul edilen karar E — Back stack/tab/hesap geçişi

- Login sonrası eski auth ekranlarına geri dönüş olmayacak; logout eski hesaba ait ana stack ve saklanmış navigation state'ini temizleyecek.
- Alt tab launchSingleTop/saveState/restoreState ve start/graph sınırları açık olacak; popUpTo(0)/boş BackHandler ile sorun gizlenmeyecek.
- Tab state koruma eski hesap state'ini koruma değildir; hesap geçişi güvenliği ilgili oturum kararıyla hizalanacak.

### 20.6 Kabul edilen karar F — Link/kalıcı uyum

- Dış reset/bildirim URI sözleşmesi korunup doğrulanacak, session readiness sonrası typed hedefe dönüşecek.
- Type safety yetkilendirme/link güvenliği/oobCode doğrulaması sağlamaz; cold/warm giriş 22. maddeyle ele alınacak.
- Kotlin Serialization plugin/runtime mevcut Kotlin ile uyumlu, katalogdan ve yalnız ihtiyaç kadar eklenecek.

### 20.7 Kapsam ve tamamlanma

- Screens/FinanceNavigation/NavExtension, auth/main graph/constants/deep link, bottom bar, top bar metadata, kart callback, Detail VM ve gerekli Gradle/catalog birlikte planlanacak.
- Eski string/typed paralel kaynak, geçersiz fallback ve yanlış graph temizlik davranışı giderilecek; URI ve persisted state/worker kimlik uyumu korunacak.
- Kullanıcının kütüphane/Manifest netleştirmesi genel çalışma sözleşmesine eklendi; gerekli değişiklikler uygulama aşamasında yapılır, şimdi yapılmaz. Güvenlik/anahtar konusu 23. madde.
- Tüm maddeler bitmeden refactor yok; Edit* istisnası ve genel tutarlılık korunur. İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur test/build/lint çalıştırılmadı.

## Madde 21 — Ölçülü API modernizasyonu

Durum: Kullanıcı altı öneriyi, koşullu seçenekleri koşullu bırakıp AI sohbet/finans analiz işlevini koruma sınırıyla kabul etti. Üretim kodu değişmedi. Güncelleme hedefi eski/sorunlu API'yi düzeltmek; bütün dependency'leri koşulsuz latest yapma değil.

### 21.1 Kabul edilen karar A — Google giriş API geçişi

- Legacy GoogleSignIn yerine Credential Manager; Firebase Authentication bağlantısı ve mevcut kullanıcı/provider politikası korunacak.
- Hesap seçimi/iptal/provider collision/logout birlikte ele alınır; salt import dönüşümü sayılmaz. UI sağlayıcı etkileşimi yeni API'nin gerçek davranışına göre değerlendirilecek.

### 21.2 Kabul edilen karar B — Gemini erişim yolunu güncelleme

- Eski Google AI client SDK yerine desteklenen Firebase AI Logic veya authenticated server proxy değerlendirilecek; kesin yol 23/30. maddelerde güvenlik/App Check/maliyet/gizlilikle birlikte seçilecek.
- Yeni backend/proxy kurulması veya Console mutasyonu bu maddeyle otomatik onaylı değildir.
- Kullanıcının şartı: AI sohbet ve finansal analiz işlevinin kullanıcı açısından çalışma amacı/akışı korunacak; değişiklik erişim katmanındadır. Yeni ürün davranışı, özellik veya hesap politikası kendiliğinden eklenmez.
- Kullanıcının netleştirdiği işleyiş sınırı: AI yanıtının metinsel içeriğinin farklı olması tek başına işleyiş değişikliği değildir. Mesaj gönderme, AI yanıtı alma, finansal analiz ve sohbet geçmişini kullanma gibi mevcut işlevler çalışmaya devam etmelidir. SDK geçişi nedeniyle önceden çalışan bir özelliğin artık çalışmaması kabul edilmez ve başarılı geçiş sayılmaz; gerekli erişim/adapter uyarlamaları bu işlevleri korumak için yapılır.
- Farklı SDK/model/platform birebir aynı response/latency/error veya yapılandırma davranışını garanti etmez; gerekli uyarlamalar açıklanır, maddi ürün/maliyet/gizlilik değişikliğinde kullanıcı kararı alınır. Aynı model/ayar/veri kapsamı mümkün olduğunca korunur; keyfi model değişikliği yok.
- Kod/SDK güncellemesinin uygulama aşamasında doğrulanması gerekir; mevcut çalışma veya future migration başarısı şimdi iddia edilmez.

### 21.3 Kabul edilen karar C — Gradle API ve dependency sahipliği

- Uygun kotlinOptions → typed compilerOptions; syntax değişikliği derleme hedefini keyfi değiştirmez.
- Doğrudan kullanılan API dependency'leri açık; kesin sürüm uyumu/tekrarlar 32. maddede.

### 21.4 Kabul edilen karar D — Tarih/konum modernizasyonu

- Uygun java.time/Clock; dönem anlamları ilgili maddede. Geocoder desteklenen platforma göre async veya main-safe blocking yol; hata/iptal tamamlanır.
- Yeni API var diye desteklenen eski cihaz yolu silinmez; minSdk30 için gereksiz ek desugaring kurulmaz.

### 21.5 Kabul edilen karar E — Fotoğraf/splash koşullu seçenekleri

- Kamera TakePicture + dar FileProvider; CameraX ekleme yok.
- GetContent geçerli; Photo Picker yalnız gerçek fayda varsa değerlendirilir, geçiş bu onayla otomatik zorunlu değildir. URI/local copy/background erişimi korunur.
- Splash yapay bekleme/session readiness düzeltilir; sistem SplashScreen API geçişi gerçek ihtiyaç açısından ayrıca değerlendirilir.

### 21.6 Kabul edilen karar F — Mevcut doğru API'leri koruma

- Lifecycle-aware toplama/uygun WhileSubscribed korunur. Maps marker ve Room işlemlerinde davranış doğruluğu öncelikli; sırf modern ad için Upsert/marker değişimi yok.
- Navigation3/Coil3/KSP veya AGP büyük sürüm/multimodule sırf yeni olduğu için kapsama eklenmez; ihtiyaç/uyumluluk gerekir.

### 21.7 Kapsam ve uygulama sınırı

- Gereken Kotlin/adapter/DI, dependency/plugin/Gradle, Manifest/XML birlikte faz planına alınır. Dış Google/Firebase Console ayarı gerekiyorsa açık belirtilir ve yetki sınırı korunur.
- Eski dependency ancak artık kullanılmadığı doğrulanarak kaldırılır; koşullu seçenekler kesin uygulanacak gibi raporlanmaz.
- Tüm maddeler bitmeden refactor yok; Edit* istisnası/genel tutarlılık korunur. İlgili testler her fazda incelenip yazılacak ve çalıştırılacak; bu tur test/build/lint çalıştırılmadı.

## Madde 22 — Manifest, XML, izinler, deep link ve backup

Durum: Kullanıcı altı öneriyi kabul etti. Üretim kodu değişmedi; izin ve yedekleme ayrıntıları gerçek kullanım/hesap güvenliği üzerinden uygulanacak, otomatik ürün tercihi yapılmayacak.

### 22.1 İzinleri gerçek kullanım ile eşleştirme

- Bildirim runtime izni ve konum coarse/fine izin akışı tutarlı yönetilecek; reddetme durumunda uygulama kontrollü davranacak.
- SCHEDULE_EXACT_ALARM mevcut WorkManager kullanımında gerekli değilse kaldırılacak. RECEIVE_BOOT_COMPLETED, WAKE_LOCK ve diğer dependency kaynaklı izinler merged manifest ve kullanım değerlendirilmeden kaldırılmayacak.
- Gereksiz izin kaldırma, çalışan bildirim/konum/zamanlama özelliğini bozmayacak; kesin zamanlama ihtiyacı ilgili ürün maddesinde ele alınacak.

### 22.2 Bileşenlerin güvenli sınırlarını koruma

- exported, FileProvider, grantUriPermissions, paylaşılabilir yollar ve PendingIntent hedef/mutability ayarları kontrol edilecek.
- Mevcut dar transaction_photos FileProvider yolu, dışa kapalı servis/receiver ve uygun immutable PendingIntent korunacak; kamera zorunlu donanım haline getirilmeyecek.
- Paket/sınıf taşımalarında Manifest referansları birlikte güncellenecek.

### 22.3 WorkManager ve AndroidX Startup

- HiltWorkerFactory ve Configuration.Provider bağlantısı korunacak. Gerekiyorsa tüm InitializationProvider yerine yalnız WorkManager başlangıç metadata kaydı kaldırılacak; diğer Startup initializer'ları gereksiz yere devre dışı bırakılmayacak.
- Nihai merged manifest ve başlangıç doğrulaması uygulama aşamasındadır; şu anda build/runtime doğrulandığı iddia edilmez.

### 22.4 Deep linklerin cold/warm girişte doğru işlenmesi

- Mevcut dış URI biçimi korunarak şifre sıfırlama ve bildirim bağlantıları uygulama giriş katmanında scheme/host/path/action parametreleriyle doğrulanacak ve typed destination'a dönüştürülecek.
- İlk açılış ve onNewIntent akışı kapsanacak; aynı intent iki kez işlenmeyecek. Şifre sıfırlama oturumsuz kullanılabilir; hesap gerektiren finansal hedefler oturum hazır olmadan açılmayacak.
- Custom scheme tek başına uygulama sahipliği/güvenlik garantisi değildir. Sahip olunan HTTPS alan adı olmadan App Links kurulumu varsayılmayacak; iç bildirimler uygun explicit hedef kullanacak.

### 22.5 Açık yedekleme ve geri yükleme politikası

- allowBackup, backup_rules ve data_extraction_rules birlikte değerlendirilecek; finansal DB, fotoğraflar, hesap tercihleri/token ve bekleyen senkronizasyon verileri ayrı ele alınacak.
- Başka hesabın verisini göstermeyen hesap izolasyonu ve güvenli geri yükleme gözetilecek. Tüm verileri otomatik dahil etme veya yedeklemeyi bütünüyle kapatma kararı verilmedi; gerekli ürün tercihi kullanıcıyla netleştirilecek.

### 22.6 XML ve kodun birlikte güncellenmesi

- Metin, tema, provider yolları ve yedekleme XML'leri refactorla uyumlu güncellenecek; gerekli Manifest/Gradle düzenlemeleri ilgili küçük uygulama fazına dahil edilecek.
- Anahtar güvenliği 23. maddede ayrıca görüşülecek. Bütün maddeler tamamlanmadan üretim refactoru yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacakna ertelendi.

## Madde 23 — API anahtarları, APK imzalama ve Git güvenliği

Durum: Kullanıcı altı öneriyi, Firebase istemci yapılandırmasının Git'te bulunmasının tek başına güvenlik açığı olmadığı açıklamasıyla kabul etti. Üretim kodu/Console/Git geçmişi değişmedi; tam güvenlik garantisi verilmedi.

### 23.1 İstemci yapılandırması ile gerçek sırları ayırma

- google-services.json Firebase istemci yapılandırmasıdır; service-account özel anahtarı değildir. Yalnız uygun Firebase servislerine kısıtlı istemci anahtarlarının Git'te bulunması kabul edilebilir; şirket politikası nedeniyle CI üzerinden verilmesi de mümkündür. Bütün şirketlerin aynı yöntemi kullandığı iddia edilmez.
- Gerçek gizli Gemini anahtarları, service-account özel anahtarları, imzalama keystore'ları ve parolalar Git'e konmayacak; sunucu sırları APK'ya gömülmeyecek. Görünen her anahtar otomatik zararsız sayılmaz; türü ve izinleri değerlendirilir.
- Firestore/Storage Rules, kullanıcı bazlı yetkilendirme ve uygun App Check politikası ayrıca değerlendirilir. İstemci dosyasını gizlemek veri erişim güvenliğinin yerine geçmez; Console kontrolleri yapılmadan güvenli olduğu onaylanmaz.

### 23.2 Maps yapılandırması ve kısıtları

- Maps anahtarı kaynak Manifest'teki literal değerden yerel/CI yapılandırması ve Manifest placeholder yoluna taşınacak; gerçek değer Git dışında tutulacak, güvenli örnek yapılandırma belgelenecek.
- Bu işlem anahtarı APK içinde gizli yapmaz. Android paket adı/imza sertifikası ve gerekli API kısıtları Console tarafında doğrulanacak; mevcut kısıtlar denetimde doğrulanmadı.

### 23.3 Gemini erişim güvenliği

- Gizli Gemini anahtarını local.properties üzerinden BuildConfig'e geçirmek güvenlik çözümü sayılmayacak; APK'ya gömülmeyecek.
- Firebase AI Logic veya authenticated proxy seçimi 30. maddede App Check, maliyet, veri gizliliği ve mevcut AI işlevlerinin korunmasıyla kesinleşecek. Yeni backend bu onayla otomatik kurulmaz.
- İncelenen Gemini yapılandırması boştu; mevcut gizli Gemini anahtarı sızıntısı kanıtlanmış gibi raporlanmayacak.

### 23.4 APK imzalama ve SHA kayıtları

- Debug keystore repo dışındadır; mevcut release signing yapılandırması yoktur. Release gerektiğinde keystore/parolalar güvenli yerel veya CI kaynağından alınacak, Git'e eklenmeyecek.
- Debug/upload/Play App Signing sertifikalarının görevleri ayrılacak; Google girişinin gereken SHA kayıtları kullanılan dağıtım/imza yoluyla eşleşecek. SHA sertifika parmak izi özel imzalama anahtarı değildir.

### 23.5 Git koruması ve geliştirme yapılandırması

- Keystore, parola dosyaları, .env ve service-account gibi sır dosyalarının ignore kuralları ve güvenli örnek kurulum dokümanı tamamlanacak.
- Zaten takip edilen dosyalara yalnız .gitignore eklemek yeterli sayılmayacak. Mevcut dosyalar ve geçmiş ayrı kontrol edilerek Git'te bulunmaması gereken sırların kalmaması hedeflenecek; Firebase istemci dosyasının tutulması tek başına kusur değildir.

### 23.6 Geçmiş, anahtar yenileme ve yetki sınırı

- Maps/Firebase yapılandırması yerel Git geçmişinde görülmüştü; canlı GitHub görünürlüğü/içeriği doğrulanmadı. Güncel dosyayı silmek geçmişi temizlemez.
- Gerçek sır ifşası varsa iptal/yenileme değerlendirilir; geçmiş temizliği tek başına sızmış sırrı güvenli yapmaz. Console değişikliği, key rotation ve Git geçmişini yeniden yazma ayrı açık yetki gerektirir; kendiliğinden yapılmayacak.
- Profesyonel güvenlik yaklaşımı hedeflenir, ancak uygulama ve gerekli kontroller tamamlanmadan hiçbir güvenlik açığı kalmadığı iddia edilmez. Tüm maddeler tamamlanmadan üretim refactoru yok; ilgili testler her faza dahildir.

## Madde 24 — Oturum, hesap değişimi ve çıkışta veri koruma

Durum: Kullanıcı yedi öneriyi ve bildirim kapsamını kabul etti. Üretim kodu değişmedi; hedef veri kaybetmeden çıkış, kesin hesap izolasyonu ve eski oturumun yeni oturuma müdahalesini önlemektir.

### 24.1 Tek sahipli oturum geçişi

- Giriş/çıkış/yeniden açılış hazırlığı uygun SessionCoordinator veya eşdeğer tek sahip üzerinden koordine edilecek; auth, sync ve temizlik ekranlara dağılmayacak. Dev bir genel manager kurulmayacak; feature sorumlulukları korunacak.

### 24.2 Hesaba ait yerel veri

- Finansal kayıtlar, AI mesajları, fotoğraflar ve bekleyen işler hesap sahibine bağlı olacak. Sorgular, UI state, dosyalar ve upload/download hesap sınırını koruyacak.
- 5 Ekim kullanıcı teyidi: Tek telefon kullanılsa bile aynı cihazda farklı hesaplarla giriş yapılabileceği kabul edilecek. Yapı tek sabit kullanıcı/UID varsaymayacak; A çıkış → B giriş sırasında A'nın kaydı, pending işi, dosyası veya geç callback'i B'ye gösterilmeyecek/gönderilmeyecek. A tekrar giriş yaptığında yalnız kendi korunmuş verisi ve bekleyen işleri devam edecek. Bu onay sahibi belirsiz eski kayıtları otomatik A/B hesabına atama kararı değildir.
- Eski hesabın verileri yeni hesapta görünmeyecek, işlenmeyecek veya yeni hesaba gönderilmeyecek. Mevcut sahibi belirsiz verilerin migration politikası uygulanmadan önce açıkça belirlenecek; keyfi kullanıcıya atanmayacak.

### 24.3 Çıkışta bekleyen veriyi koruma

- Senkronlanmamış kayıtlar otomatik silinmeyecek; aynı hesaba ait yerel alanda korunacak ve o hesap yeniden giriş yaptığında senkronizasyon devam edecek.
- Gerekirse bu cihazdaki verileri kaldır ayrı, açık kullanıcı işlemi olacak; logout ile otomatik eşitlenmeyecek. Saklanan hesap verisi aktif olmayan hesaptan erişilebilir hale getirilmeyecek.

### 24.4 Eski oturum işlerini durdurma

- Hesap listener'ları/session job'ları kapatılacak; geç callback/sonuçlar UID ve oturum nesli kontrolüyle yeni hesabı değiştiremeyecek.
- cancelAllWork yerine ilgili hesabın iş kimlikleri/tag'leri yönetilecek; başka amaçlı işler iptal edilmeyecek. Kalıcı pending verinin korunması işin aktif çalışmasını sürdürmek anlamına gelmez.

### 24.5 Logout/login yarışını önleme

- Kontrolsüz fire-and-forget clearAllTables kaldırılacak. UI eski hesaptan hemen ayrılacak; gerekli yerel geçiş tamamlanmadan yeni hesabın veri akışı başlamayacak.
- Oturum geçişleri seri ve tamamlanması tanımlı olacak; eski temizlik yeni login/ilk sync verilerini silemeyecek. Merkezi ApplicationScope'a taşımak tek başına bu yarışı çözmez.

### 24.6 Başlangıç ve auth hazırlığı

- Oturum kontrolü, gereken e-posta doğrulaması ve yerel hesap hazırlığı finansal ekran girişinde tutarlı korunacak; splash keyfi delay yerine hazırlık durumunu izleyecek.
- Yerel hesap verisi hazırsa uzak sync beklemek offline kullanımı gereksiz engellemeyecek; auth güvenlik gereksinimleri atlanmayacak.

### 24.7 Token ve bildirimlerin hesap izolasyonu

- Token'ın eski hesap bağlantısının kaldırılması/yeni hesapla kaydı doğru sırada yönetilecek. Ağ hatası logout'u süresiz bekletmeyecek; gerekiyorsa uzak işlem eski hesap kimliği korunarak tekrar denenecek. Sunucu yetkilendirmesi/yetki gerektiren değişiklik ayrıca değerlendirilir.
- Çıkış yapan hesabın zamanlanmış bildirim işleri durdurulacak ve gösterilmiş hesap bildirimleri temizlenecek. Geç eski hesap FCM mesajı yeni hesapta gösterilmeyecek veya veri değiştirmeyecek; gereken hesap kimliği payload sözleşmesi 27. maddede ele alınacak.
- Bildirime tıklama/tamamlama aksiyonu yanlış hesap ekranını açmayacak veya finansal kaydını işlemeyecek. Aynı hesap tekrar giriş yaptığında halen geçerli bildirim planları yeniden kurulacak; gerekli missed/due politikası 27. maddede netleşecek.
- Bildirimi/işi temizlemek finansal kaydı silmek değildir. Ayrıntılı WorkManager/FCM düzenlemesi 27. maddede görüşülecek.

Tüm başlıklar kararlaştırılmadan üretim refactoru yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacak.

## Madde 25 — Room, kayıt kimlikleri ve offline senkronizasyon

Durum: Kullanıcı yedi öneriyi kabul etti. Üretim kodu değişmedi; kalıcı veri ve sync doğruluğu hedeflenir, conflict/çok cihazlı işlem ürün politikası otomatik seçilmez.

### 25.1 Yerel ve uzak kayıt kimliği

- Room insert sonucunun gerçek ID'si kullanılacak; uzak güncellemeyi yerel kayda eşlerken mevcut local primary key korunacak. Varsayılan 0 ID ile yanlış satır güncelleme engellenecek.
- Hesap + uzak kayıt kimliği uygun tekillik ile korunacak; aynı remote kaydın farklı local ID'lerle çoğalması önlenecek. Yerel/uzak kimliğin rolleri açık kalacak.

### 25.2 Kalıcı bekleyen işlem ve atomik yerel değişiklik

- Ekleme/düzenleme/silme niyeti Room'da kalıcı tutulacak; yerel değişiklik ve gerekli pending operation tek transaction içinde kaydedilecek.
- Bekleyen iş hesap/record/operation kimliğini ve gerektiği kadar sürüm bilgisini taşıyacak; ekran kapanışı/process death niyeti kaybettirmeyecek. İhtiyaçtan büyük generic event framework kurulmayacak.

### 25.3 Dayanıklı WorkManager senkronizasyonu

- Ağ constraint, retry/backoff ve geçici/kalıcı hata ayrımı uygulanacak; kalıcı hata sonsuz retry'ya dönmeyecek. İşler hesap sınırı ve geçerli oturum/yetkiyle çalışacak.
- Pending kayıtların okunup gönderilmesi tekrar çalışmaya dayanıklı olacak; background başarı yalnız doğrulanmış uzak işlem tamamlanmasıyla işaretlenecek. WorkManager process-lifetime coroutine yerine kalıcılık içindir, uzakta exactly-once garantisi değildir.

### 25.4 Silme niyetini ve sonuç doğruluğunu koruma

- Silme niyeti uzak silme tamamlanana kadar korunacak; silinen kayıt yeniden pull ile görünmeyecek. Fiziksel yerel silmenin zamanı açık politika ile belirlenir.
- Başarısız upload/update/delete başarı sayılmayacak; local saved/pending ve remote synced ayrı sonuçlar olacak.

### 25.5 İlk ve devam eden uzlaştırma

- Yalnız yerelde yoksa insert yerine uzak güncelleme/silme de ele alınacak; pending yerel değişiklikler kör snapshot veya upsert ile ezilmeyecek.
- Açık sürüm/silme bilgisi ve deterministik reconciliation kullanılacak. Kullanıcı 5 Ekim'de gerçek çatışmada kullanıcı seçimini onayladı; keyfi last-write-wins veya her durumda yerel kazanır uygulanmayacak. Çok cihazlı finansal/idempotent geçiş gerektiğinde server transactional sözleşmesi ayrıca ele alınır; client Mutex yeterli sayılmaz, yeni backend kendiliğinden kurulmaz.
- Yalnız yerel değişiklik varsa gönderilecek; yalnız uzak değişiklik varsa uygulanacak; iki taraftaki sonuç aynıysa gereksiz seçim sorulmayacak. Aynı kaydın aynı alanında bağımsız farklı değişiklik veya düzenleme/silme çatışması varsa iki sürüm ve pending niyet seçim yapılana kadar korunacak, sessizce ezme/silme veya otomatik yeniden oluşturma olmayacak.
- Yalnız gerçek çatışma için anlaşılır seçim diyaloğu bu kullanıcı onayının sınırlı tasarım/akış istisnasıdır. Seçilen çözüm gönderilmeden güncel uzak sürüm tekrar doğrulanacak; arada yeni değişiklik varsa kör overwrite yapılmayacak. Hesap sınırı her adımda korunacak; başka hesabın verisi uzlaştırılmayacak. Karar kaydedildi, henüz uygulanıp test edilmedi.

### 25.6 Migration, schema ve veri invariant'ları

- Schema export ve mevcut veriyi koruyan gerçek migration hazırlanacak; fallbackToDestructiveMigration'ı yalnız kaldırmak tamamlanmış migration sayılmayacak.
- Gerekli index/unique kısıtları mevcut tekrarlar ve account ownership migration'ı dikkate alınarak kurulacak. Genel/kategori bütçesi tekilliği nullable kategori durumuyla birlikte tasarlanacak; sahadaki/verili sürümler değerlendirilmeden veri silinmeyecek.

### 25.7 Sync sorumluluklarını ayırma

- Büyük FirebaseSyncService içindeki kayıt türüne özel mapping/sync ilgili feature data katmanına ayrılacak. Ortak hesap/sync koordinasyonu küçük kalacak; anlamlı mevcut ortak helper korunacak.
- Her kayıt türünü zorla aynılaştıran dev generic adapter/manager yapılmayacak. Önceki mimari/duplication/scope kararlarıyla birlikte uygulanacak.

Tüm maddeler tamamlanmadan üretim refactoru yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacak.

## Madde 26 — Para, bütçe ve tarih hesaplamaları

Durum: Kullanıcı yedi öneriyi kabul etti. Üretim kodu değişmedi; kesin para birimi ve bütçe yüzde anlamı uygulamadan önce kullanıcıyla netleştirilecek, mevcut veriler keyfi yorumlanmayacak.

### 26.1 Para biriminin açık sahipliği

- 5 Ekim kullanıcı kararı: sabit TRY zorunluluğu yok. Yeni hesabın ilk para birimi cihazın bölge ayarından belirlenecek ve hesaba bağlı currencyCode olarak saklanacak. Dil tek başına para birimi seçmez; aynı hesap başka cihazda mevcut tercihini kullanacak, yeniden cihazdan türetilmeyecek.
- Sonraki dil/bölge değişiklikleri yalnız sayı/gösterim biçimini değiştirecek; mevcut tutarın para birimi değişmeyecek. 100 TL, dil değişince 100 USD sayılmayacak. Otomatik döviz dönüşümü, kur servisi veya hesap içinde çoklu para birimi özelliği bu onaya dahil değildir.
- Son kullanıcı kararı: uygulama yayımlanmayacak; eski geliştirme kayıtlarının parasal anlamının korunması öncelik değil. Yeni kullanıcıların doğru başlangıcı esas alınacak; eski tutarların para birimini doğrulamak geçişi bloke etmeyecek. Bu karar eski verileri/DB'yi silme veya destructive migration izni değildir; gerekiyorsa somut temizlik ayrıca onaya sunulur. Yeni hesabın cihaz bölgesinden belirlenen para birimi hesapta korunur. Bölgeden geçerli para birimi belirlenemeyen durum için açık fallback/teyit davranışı uygulama planında netleştirilecek; sessiz TRY varsayımı yok.

### 26.2 Hassas para temsili ve veri geçişi

- Hassas temsil hedefi seçilen para birimiyle birlikte ele alınacak: Long alt birim ölçeği para biriminin ondalık basamak sayısına bağlıdır; bütün paralar kuruş veya iki basamak kabul edilmeyecek. Örneğin TRY için 125,50 TL = 12550 kuruş. Kesin migration/yuvarlama sınırları uygulamadan önce netleştirilecek. BigDecimal parsing/yuvarlama gerektiği sınırda kullanılabilir; uygulama çapında gereksiz decimal framework kurulmaz.
- Form/Room/Firestore/mapper/rapor ve mevcut veriler birlikte dönüştürülecek; yalnız toLong ile ölçek kaybettiren dönüşüm yapılmayacak. Yuvarlama, üst sınır ve eski/yenilenmiş kayıt uyumu uygulama planında açık olacak.

### 26.3 Ortak tutar doğrulaması

- Ekleme/düzenleme/bütçe eşdeğer alanları aynı parsing ve doğrulama kurallarını kullanacak: virgül/nokta, tutar geçerliliği, hassasiyet ve üst sınır.
- Geçersiz girdiler sessizce sıfıra düşmeyecek; mevcut Double sınırında NaN/Infinity gibi değerler kabul edilmeyecek. Düzenlemede toInt kaynaklı küsurat kaybı giderilecek. UI filtresi domain doğrulamasının yerine geçmez; hata ilgili alanla ilişkilendirilir.

### 26.4 Bütçe kuralları

- Sabit tutar/yüzde bütçesinin gerçek anlamı ve yüzde tabanı kullanıcıyla belirlenecek; yüzde aralığı/toplam yüzde kuralı otomatik seçilmedi.
- Home/Budget/AI aynı amaçlı hesapta ortak saf Kotlin hesaplama kurallarını kullanacak; yüzde bütçesini sabit tutar gibi yorumlama giderilecek.

### 26.5 Finansal dönem tutarlılığı

- Bu ay/önceki ay/son bir ay etiketleri gerçek aralıkla eşleşecek. Aynı isimli dönem Home/Budget/AI'da aynı sınırları kullanacak; tüm geçmiş masraf aylık bütçeyle yanlış karşılaştırılmayacak.
- Gerekli küçük FinancePeriod/DateRange ve ortak FinancialSummary/BudgetCalculator kullanılacak; farklı amaçlı raporlar aynı döneme zorlanmayacak, gereksiz framework yok.

### 26.6 Tarih ve zaman sınırları

- Uygun java.time LocalDate/Instant/ZoneId ve ihtiyaçlı inject Clock kullanılacak. Date-only seçim ile gerçek dueAt ayrılacak; UTC date picker/local gün dönüşümü açık olacak.
- DAO tarih aralıkları tutarlı başlangıç/dışlanan bitiş sınırıyla ele alınacak. Gün/ay değişimi ve resume sonrası gereken dönem tazelemesi yapılacak; eski VM aralığı kalmayacak. Gelecek tarihe yanlış Bugün gösterimi giderilecek; locale/timezone değişimi dikkate alınacak.

### 26.7 Oran adı ve finansal gösterim

- Harcanan/kalan gelir oranı fonksiyon adı, formül ve UI etiketiyle aynı anlama gelecek. Sıfır gelir, negatif bakiye ve bütçe aşımı açık işlenecek.
- Progress çizimi gerekirse clamp edilir, fakat bu finansal aşımı/eksi durumu gizlemeyecek. Görsel ile gösterilen değer çelişmeyecek.

Tüm başlıklar kararlaştırılmadan üretim refactoru yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacak.

## Madde 27 — Planlanan işlemler, WorkManager ve FCM

Durum: Kullanıcı sekiz öneriyi kabul etti. Üretim kodu değişmedi; kesin saat, tekrar/erteleme ve süresi geçen finansal planın davranışı ayrıca netleşecek. İstemci refactoru dış sunucu/Console değişikliğini otomatik yetkilendirmez.

### 27.1 Doğru hedef zaman

- Gelecek işlem birkaç saniye sonra bildirim üretmeyecek; gerçek dueAt üzerinden başlangıç gecikmesi hesaplanacak. Worker çalışırken hesap, tarih ve kayıt durumu yeniden doğrulanacak.
- WorkManager kesin saat garantisi sayılmayacak. Exact alarm yalnız gerçek ürün ihtiyacı ve izin/politika değerlendirmesi varsa seçilecek; otomatik eklenmez.

### 27.2 Ortak atomik tamamlama

- Ekran ve notification action aynı CompleteScheduledTransaction iş kuralını çağıracak. Room transaction ve uygun completedFromScheduledId/operation kimliği tekilliğiyle çift tıklama/eşzamanlı tetikleme tekrarlı finansal kayıt üretmeyecek.
- Finansal kayıt, scheduled durum geçişi, pending sync ve medya devri tutarlı sözleşmeye bağlanacak; bildirim iptali tekrar çalışmaya dayanıklı olacak.
- Çok cihazlı tamamlama için uzak atomik/idempotent invariant ayrıca gerekir; yalnız local Mutex/unique kısıtı global garanti sayılmaz.

### 27.3 Bildirim ömrü ile finansal planı ayırma

- Bildirim temizliği ile finansal plan yaşam döngüsü ayrı sorumluluk olacak. Mevcut DeleteExpiredNotification'ın finansal kayıt silmesi açık ürün politikasıyla ele alınacak.
- Süresi geçen planın korunması, Expired durumuna alınması veya silinmesi kesinleştirilmedi; kullanıcı kararı olmadan mevcut davranış keyfi değiştirilmeyecek. Bildirim kaldırma tek başına finansal veri silme komutu olmayacak; onaylı otomatik plan silme varsa ayrı açık iş kuralı gerekecek.

### 27.4 İş ve olay tekrarlarını önleme

- Küçük ReminderScheduler ve hesap+kayıt bazlı work name/tag/input sözleşmesi kullanılacak. Tamamlama/silme/logout ilgili zinciri iptal edecek; ilgisiz işler korunacak.
- Yerel zamanlama ve FCM aynı olay için kalıcı event/version dedupe sınırı kullanacak. Notification ID üretmek tek başına iş tekrarını önlemez; FCM teslim garantisi vaat edilmez.

### 27.5 Receiver ve servis iş ömrü

- Kısa yerel kritik receiver işi gerektiğinde goAsync ve finally finish/süre sınırıyla tamamlanacak; uzun/ağ gerektiren işlem account-aware kalıcı worker'a devredilecek.
- FCM callback/service içinde kontrolsüz scope işi bırakılmayacak; işin doğru ömrü ve iptali tanımlanacak. Bildirimi kapatmak finansal komutun kaydedildiği/başarılı olduğu anlamına gelmeyecek; hata/yeniden deneme kullanıcı durumuyla tutarlı olacak.

### 27.6 İzin ve kilit ekranı gizliliği

- Bildirim runtime izni anlamlı noktada istenecek, red/settings yolu yönetilecek; worker permission/channel durumunda güvenli davranacak.
- Finansal tutar/kategori lockscreen gösterimi private/publicVersion ve açık ürün gizlilik politikasıyla değerlendirilecek. Kullanıcı metninde enum.name gibi teknik değerler yerine XML karşılıkları kullanılacak.

### 27.7 FCM payload ve token sözleşmesi

- Typed payload/schema validation, user/account match, record/event kimliği ve mesaj türü doğrulanacak. Eksik hesap kimliği kontrolsüz kabul edilmeyecek; geç eski hesap mesajı veya duplicate işlem gösterim/veri değişikliği üretmeyecek.
- Yerel kayıt henüz yoksa olay sessizce kaybolmayacak; geçici gecikme ile kalıcı eksik/silinmiş kayıt ayrılıp uygun kalıcı iş/retry politikası uygulanacak.
- onNewToken'da verilen token kullanılacak; hesap bağlı upsert/revoke, request-generation, gözlemlenebilir hata ve retry ele alınacak. Sunucu eski token temizliği/payload yetkilendirmesi ayrıca doğrulanacak. Sistem tarafından otomatik gösterilen notification payload'ları yalnız client guard ile güvenli sayılmayacak; gönderim sözleşmesi gerekli yetkiyle ayrıca ele alınacak.

### 27.8 Erteleme/iptal/dismiss anlamları

- Bildirimi kapat, sonra hatırlat ve planı iptal et ayrı iş kuralları olacak; ACTION_CANCEL'ın aslında snooze olması gibi anlam karışıklıkları giderilecek. Sınıf/fonksiyon adları gerçek görevi anlatacak; FCMNotificationSender adı gerçek remote reminder işlemiyle uyumlu hale getirilecek.
- Remote snooze/delete-create yarışları deterministic kimlik veya uygun atomik sunucu sözleşmesiyle değerlendirilecek. Tekrar aralığı, missed reminder ve sona erme kararı kullanıcıyla netleşecek; client'a server credential konmayacak.

Tüm başlıklar kararlaştırılmadan üretim refactoru yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacak.

## Madde 28 — Fotoğraf, kamera ve dosya yönetimi

Durum: Kullanıcı yedi öneriyi ve ortak medya hazırlama/okunabilirliği koruyan sıkıştırma kararlarını kabul etti. Üretim kodu değişmedi; hedef mevcut çekme/seçme işlevini koruyarak veri kaybı, gereksiz indirme ve disk birikimini önlemektir.

### 28.1 Kamera/galeri UI yaşam süresi

- Activity/launcher/helper referansları ViewModel'de saklanmayacak; Route UI etkileşimini, ViewModel medya sonucunu/draft durumunu yönetecek.
- TakePicture + dar FileProvider korunacak; CameraX eklenmeyecek. Mevcut GetContent geçerli; Photo Picker geçişi 21. maddedeki koşullu seçenek olarak kalır.

### 28.2 İzin, iptal ve başarısızlık

- Ekleme/düzenleme ekranları aynı gerekli izin/iptal kurallarını kullanacak; izin verildikten sonra gereken kamera akışı doğru devam edecek. Dış kamera intent'i için CAMERA izninin gerçekten gerekli olup olmadığı Manifest ile birlikte değerlendirilecek.
- İptal/başarısız çekimde referanssız geçici dosya temizlenecek; mevcut kayıt veya pending upload dosyası silinmeyecek. Hata kullanıcıya uygun alanda bildirilecek.

### 28.3 Güvenli fotoğraf değiştirme

- Yeni dosya temporary/atomic save ile hazırlanacak, DB referansı başarıyla güncellenecek, ardından kullanılmayan eski dosya temizlenecek. Yeni dosya kaydedilemezse eski fotoğraf korunacak.
- Yerel commit sonrası scheduling hatası yerel kayıt/fotoğrafı kontrolsüz silmeyecek; saved/pending sonucu ayrı ele alınacak. Upload'ın kullandığı dosya tamamlanana kadar korunacak.

### 28.4 Kayıt ve upload sırası

- Yerel kayıt ve pending medya bilgisi önce kalıcılaşacak; remote upsert, upload ve remote referans bağlama tutarlı sırada yürütülecek. Hesap/record/version ve pending delete kontrolü korunacak.
- Henüz oluşmamış belge başarı sayılmayacak; gerçekten silinmiş kayıt sonsuz retry'ya girmeyecek. Upload ile referans güncelleme hatası ayrı izlenecek, tekrar deneme duplicate/veri kaybı üretmeyecek.

### 28.5 Main-safe ve kontrollü görüntü işleme

- Dosya IO uygun dispatcher'da, ağır görüntü CPU işlemi gerektiğinde Default'ta yürütülecek. Küçük path/UUID üretimi için gereksiz context switch yapılmayacak.
- Büyük görüntü gereksiz full decode yerine sampled/uygun boyutta decode edilecek; EXIF yönü, boyut sınırları ve stream use/close doğru yönetilecek.

### 28.6 Yerel ve uzak medya kimliği

- Seçilen URI, kalıcı local path ve remote storage reference/version ayrı rollere sahip olacak; URL ile local path karşılaştırması değişiklik tespiti sayılmayacak.
- Yerel kayıt indirmeden önce kurulacak; aynı remote sürüm tekrar tekrar indirilmemesi için uygun deterministic cache/dedupe kullanılacak. Geç upload/download doğru hesap ve kayıt nesline bağlı sonuç yazacak.

### 28.7 Güvenli disk temizliği

- Referanssız temporary/eski cache dosyaları temizlenecek; kayıtların kullandığı ve pending upload dosyaları korunacak. Hesap dosyaları ayrılacak; logout'ta pending veri koruma fotoğraf için de geçerli olacak.
- Eski emulator insufficient storage hatasının bu koddan kaynaklandığı kanıtlanmış gibi raporlanmayacak; bu ayrı statik disk büyümesi riskidir.

### 28.8 Ortak medya hazırlama ve ölçülü sıkıştırma

- Kamera ve galeriden alınan görseller ortak doğrulama/hazırlama akışından geçecek: desteklenen içerik doğrulaması, bozuk dosya kontrolü, yön düzeltme, ihtiyaçlı boyutlandırma ve format/kalite seçimi.
- Büyük fotoğraf oranı korunarak küçültülecek; küçük fotoğraf gereksiz büyütülmeyecek. Fiş/belge yazılarının okunabilirliği korunacak; aşırı kayıplı veya tekrar tekrar sıkıştırma yapılmayacak.
- Saklama/upload çözünürlük ve dosya sınırı ile liste/detay gösterim boyutu ayrıdır. Her cihaza/amacına uyan tek sabit boyut varsayılmayacak; kesin format/kalite/piksel/byte sınırları okunabilirlik ve kullanım ihtiyacına göre uygulama planında belirlenecek. Her görüntüyü aynı hedef byte'a zorlayan kontrolsüz döngü kurulmayacak.
- Aynı dosya her gösterim veya sync'te yeniden işlenmeyecek; hazırlanan kalıcı medya tekrar kullanılacak. Mevcut Coil gösterim akışıyla uyumlu kalacak; yeni sıkıştırma kütüphanesi gerçek ihtiyaç yoksa eklenmeyecek.
- Bu yaklaşım profesyonel görüntü işleme prensipleriyle uyumludur; bütün şirketlerin aynı kalite/boyut değerlerini kullandığı veya evrensel şirket standardı olduğu iddia edilmez.

Tüm başlıklar kararlaştırılmadan üretim refactoru yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacak.

## Madde 29 — Konum ve harita akışı

Durum: Kullanıcı yedi öneriyi kabul etti. Üretim kodu değişmedi; mevcut konum seçme işlevi korunacak, gereksiz yeni harita servisi veya konum framework'ü eklenmeyecek.

### 29.1 Konum ve adres çözümlemenin sahipliği

- Cihaz konumu alma ile koordinattan adres çözme ayrı, dar sorumluluklar olacak; gerekli LocationGateway/AddressResolver sınırları DI üzerinden kullanılabilecek. SDK işlemleri ekranlara dağılmayacak; gereksiz helper/interface katmanları kurulmayacak.

### 29.2 Main-safe adres çözümleme ve tamamlanma

- Desteklenen platformlarda asenkron Geocoder, eski desteklenen cihazlarda uygun dispatcher ile main-safe yol kullanılacak.
- Başarı, onError ve iptal terminal yolları eksiksiz yönetilecek; askıda continuation veya birden fazla resume olmayacak. Konum Task/callback bekleme ve CancellationTokenSource yaşam süresi çağıran işlemin iptaliyle uyumlu olacak.

### 29.3 Son seçim ve geç sonuç koruması

- Ardışık arama/konum seçimlerinde önceki iş uygun şekilde iptal edilecek; iptal edilemeyen veya geç gelen sonuç güncel koordinat/istek kimliğiyle doğrulanacak.
- Eski adres cevabı yeni konum seçimini ezmeyecek. Uygun latest-job yaklaşımı kullanılacak; finansal işlemlere genellenen kontrolsüz cancel politikası olmayacak.

### 29.4 Yaklaşık/hassas izin tutarlılığı

- Coarse/fine değerlendirmesi ilk açılış, resume ve izin callback'inde aynı kurallarla yapılacak; yaklaşık izin tamamen izinsiz gibi yorumlanmayacak.
- Red/kalıcı red ve ayarlardan dönüş akışı kontrollü ele alınacak. İzin isteme, sistem etkileşimi ve settings açma Route'a ait kalacak.

### 29.5 Seçili konum ve marker

- Arama, haritaya dokunma ve marker sürükleme aynı seçili konum state'ine bağlanacak; başlangıç konumuna bağlı remember nedeniyle marker eski yerde kalmayacak.
- Seçili koordinat, adres çözümleme ve marker güncellemesi tek yönlü ve tutarlı olacak; Compose/Maps marker API'si sırf yeni olduğu için değiştirilmez, davranış doğruluğu esas alınır.

### 29.6 Kullanıcıya açık sonuçlar

- Konum alınamaması, adres bulunamaması ve servis hatası sessiz kalmayacak; yüklenme/boş sonuç/hata ayrılacak ve uygun mesaj/yeniden deneme gösterilecek.
- Hata metinleri 18. maddedeki typed failure/presentation/XML düzeniyle uyumlu olacak; SDK exception metni doğrudan kullanıcıya aktarılmayacak.

### 29.7 UI ve kalıcı kayıt sınırı

- UI izin/ayar/harita etkileşimi Route'ta, konum verisi ve gerekli iş akışı uygun feature katmanında olacak. Koordinatlar ile adres gösterim metni birbirinin yerine kullanılmayacak.
- İptal edilen seçim mevcut kalıcı kayıtlı konumu değiştirmeyecek; onaylanan seçim draft/kayıt sözleşmesi üzerinden aktarılacak. Mevcut akış korunacak.

Tüm başlıklar kararlaştırılmadan üretim refactoru yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacak.

## Madde 30 — Yapay zekâ entegrasyonu

Durum: Kullanıcı sekiz önerinin tamamını, Firebase AI Logic geçişi dahil kabul etti. Önceki eski SDK'yı tutma/1. öneriyi erteleme değerlendirmesi nihai karar değildir. Üretim kodu değişmedi; sohbet/yanıt alma/finansal analiz/kayıtlı geçmiş işlevleri korunacak. Yeni özel backend kurulmayacak.

### 30.1 Firebase AI Logic erişim yolu

- Eski Google AI client SDK yerine Firebase AI Logic, Gemini Developer API sağlayıcısıyla kullanılacak. AI SDK dependency, istemci oluşturma, Hilt ve request/response adapter kodu birlikte uyarlanacak; gizli Gemini anahtarı Android koduna/local.properties/BuildConfig'e konmayacak. Yalnız artık kullanılmayan Gemini BuildConfig alanı kaldırılacak; BuildConfig'in tamamı kaldırılmaz.
- Kullanıcının paylaştığı Console ekranlarında Gemini Developer API Enabled, Agent Platform Not enabled, Spark plan ve AI monitoring kapalı görüldü. Bu, API etkinleştirme kanıtıdır; uygulama entegrasyonu/yanıt başarısı veya App Check tamamlandı anlamına gelmez.
- Ücretsiz kotası olan uygun model seçilecek; Spark/free-tier kullanım sınırları geçerlidir, sınırsız ücretsiz kullanım vaat edilmez. Billing/Blaze/Agent Platform kendiliğinden etkinleştirilmeyecek; ücretli değişiklik ayrıca onay gerektirir.
- App Check geliştirme kurulumu kod ve Console tarafında tamamlanacak; gereken debug token Git'e/log paylaşımına konmayacak. Yeni backend yalnız sonradan gerçek kullanıcı kota/yetkilendirme ihtiyacıyla ayrıca kararlaştırılabilir.

### 30.2 Feature-owned sorumluluklar ve DI

- AI erişimi, sohbet kalıcı kaydı ve finansal hesaplama ayrı anlamlı sorumluluklar olacak; SDK data katmanında, SDK'sız sözleşmeler domain'de kalacak. AI Hilt bağlantıları feature/ai/di altında sahiplenilecek.
- 3/7. maddelerin DI ilkesi teyit edildi: bütün Hilt modülleri tek dosyada toplanmaz; feature'a özel bağlantılar ilgili feature di, ortak altyapı core, uygulama birleştirmesi gerektiğinde app di. Aynı Hilt grafiğinde çalışırlar; boş di paketi veya constructor injection yeterliyken gereksiz provider yazılmaz.

### 30.3 Model çağrısını uzak history sync'ten ayırma

- Kullanıcı mesajı stable ID ile yerelde kaydedilecek; uzak history sync kalıcı pending akışında yürütülecek. Firestore gecikmesi model çağrısını gereksiz bekletmeyecek.
- Model cevabı yerel/pending sözleşmesine uygun kaydedilecek. AI internet gerektirir; tüm uygulama offline diye AI yanıtı da offline garanti edilmez.

### 30.4 Gönderim, sonuç ve hata

- Kontrolsüz paralel gönderim/loading yarışları uygun send guard veya açık ihtiyaçlı sıra ile önlenecek; request/reply kimlik ilişkisi korunacak, finally/cancellation doğru yönetilecek.
- SDK hatası normal AI cevabı gibi kaydedilmeyecek; typed hata ve retryable failed durum ayrılacak. Yeniden deneme aynı kullanıcı mesajını veya cevabı gereksiz çoğaltmayacak. Hesap değişiminde geç sonuçlar reddedilecek.

### 30.5 Finansal veri hazırlama ve minimizasyon

- Home/Budget ile aynı saf finansal hesap kuralları ve ilgili dönem snapshot'ı kullanılacak; bütün geçmiş/özel notlar sınırsız taşınmayacak. Budget-only kullanıcı bilgisi yanlışlıkla atlanmayacak.
- Veri kapsamı küçültme finansal analiz işlevini sessizce değiştirmeyecek; hangi bilgilerin dış servise gönderileceği ve gerekli gizlilik bilgilendirmesi açıkça belirlenecek. Tüm geçmiş analizi gibi mevcut talep varsa gerekli veri kapsamı korunacak/uygun sınırla ayrıca kararlaştırılacak.

### 30.6 Talimat ve kullanıcı verisi ayrımı

- System instruction ile kullanıcı mesajı/finansal notlar ayrılacak; notlar güvenilmeyen veri olarak yapılandırılacak. Prompt hazırlamanın tek sahibi olacak.
- Finansal toplam/limit hesabının doğruluğu modele bırakılmayacak; model güvenlik talimatı tek başına prompt injection veya hatasız cevap garantisi sayılmayacak.

### 30.7 Kayıtlı geçmiş ve model bağlamı

- Kayıtlı geçmiş görüntüleme korunacak. Mevcut çağrılar önceki soruları konuşma bağlamı olarak göndermiyor; modele sohbet hafızası eklemek ayrı ürün kararıdır, otomatik eklenmez.
- createdAt/updatedAt/sync version rolleri ayrılacak; sync mesaj oluşturulma zamanını değiştirmeyecek. Gereksiz companion/global auto-prompt yerine açık hesap/ekran sahibi 31. maddede ele alınacak. Geçmiş sessizce silinmeyecek; context/retention/pagination sınırları ihtiyaca göre seçilecek.

### 30.8 Maliyet, gizlilik ve kurulum sorumluluğu

- Model, girdi/çıktı sınırı, kota ve sağlayıcı veri kullanım koşulları uygulama öncesi değerlendirilir. Finansal prompt/yanıtlar loglara dökülmez; ücretsiz katmanın veri kullanım koşulları ücretli katmanla aynı varsayılmaz.
- App Check uygulama doğrulama içindir; tek başına kullanıcı yetkilendirmesi veya tam maliyet kontrolü sayılmaz. Auth enforcement gerekiyorsa SDK credential akışı hazırlandıktan sonra Console'da ayrıca ele alınır; template-only mode ihtiyacı olmadan etkinleştirilmez.
- Sırf AI Logic etkinleştirme nedeniyle google-services.json'ın değişmesi zorunlu değildir; mevcut proje/app bağlantısı entegrasyonda doğrulanacak, gerçekten gereken config değişikliği açıkça bildirilecek. Gemini anahtarı kopyalanmayacak.
- Android düzenlemelerini asistan yapacak. Ek Console ayarı/izin/App Check debug token kaydı gerekirse kullanıcıya adım adım ayrıca söylenecek; Console erişimi varmış veya bütün dış ayarlar tamamlanmış gibi davranılmayacak. Kullanıcı gerekli Console adımlarını ve en son cihazda yanıt doğrulamasını yapabilecek.

Tüm maddeler tamamlanmadan üretim refactoru yok; ilgili testler her fazda incelenip yazılacak ve çalıştırılacak. Console etkinleştirme kullanıcı tarafından yapıldı; Android entegrasyon başarısı henüz doğrulanmadı.

## Madde 31 — Performans, erişilebilirlik ve state restorasyonu

Durum: Kullanıcı yedi öneriyi kabul etti ve tüm refactor için tasarım/işleyiş korunmasını ayrıca şart koştu. Üretim kodu değişmedi. Bu madde görünümü veya akışı değiştiren uygulama tercihlerine otomatik onay değildir.

### 31.1 Ölçülü performans düzenlemesi

- Grafik çiziminde yinelenen nesne üretimi ve gereksiz tekrar hesaplamalar uygun sahipte düzenlenecek. Her yere remember/stability annotation eklenmeyecek; güncel veri/locale/tema davranışı korunacak.
- Görsel grafik çıktısı/animasyon veya hesap anlamı performans gerekçesiyle değiştirilmeyecek; ölçülmemiş performans kazanımı iddia edilmeyecek.

### 31.2 Listelerde kimlik ve büyüme

- Gerekli lazy list item key'leri gerçek kararlı kayıt kimliğinden sağlanacak. Liste sırası ve mevcut scroll/selection davranışı korunacak.
- Veri miktarı ve kullanım ihtiyacı olmadan Paging kurulmayacak; pagination gerekiyorsa kullanıcı deneyimi etkisi uygulama öncesinde ayrıca netleştirilecek.

### 31.3 Boyut, font scale, IME ve insets

- Sabit layout/klavye altında kalan içerik/kaydırma sorunları için tasarımı koruyan teknik çözüm araştırılacak; normal mevcut görünüm aynen korunacak.
- Genişlik/padding/scroll veya adaptive yerleşim mevcut görsel sonucu değiştiriyorsa somut etkisi kullanıcıya sunulup ayrıca onay alınacak. Responsive iyileştirme adı altında yeniden tasarım yok.

### 31.4 Erişilebilirlik

- Anlamlı ikon/action açıklamaları, field error semantics ve progress bilgisi; grafik için kategori/tutar erişilebilir açıklaması uygun yerde sağlanacak.
- Görsel olmayan semantics iyileştirmeleri mevcut görünümü koruyacak. Dokunma alanı/legend/metin/renk/kontrast düzenlemesi görünen tasarımı etkiliyorsa ayrıca onay gerektirir; bu madde renk veya layout değiştirme izni değildir.

### 31.5 Modifier sahipliği

- Root Modifier yalnız uygun root'a uygulanacak; dış padding/weight/semantics çocuklara yanlışlıkla tekrar taşınmayacak. Çocuklar uygun kendi Modifier'ını kullanacak.
- Hatalı tekrarın kaldırılması mevcut görünümü etkiliyorsa refactor ile görsel hata düzeltmesi ayrılıp kullanıcıyla netleştirilecek.

### 31.6 Süreç yeniden oluşturulmasında state

- Gereken filtre/seçim/form draft için SavedStateHandle/rememberSaveable uygun şekilde kullanılacak; finansal kalıcı veri Room'da kalacak. Bitmap/Activity/Context veya büyük liste saved state'e konmayacak; hesap değişiminde eski state sızmayacak.
- Mevcut yeniden açılış/geri dönüş davranışını değiştiren yeni draft veya restoration politikası kendiliğinden eklenmeyecek; hangi state'in hangi ömürde korunacağı kullanıcı akışını koruma şartıyla netleşecek.

### 31.7 Global geçici state'in açık sahipliği

- Ai pendingAutoPrompt gibi global/companion alan yerine gerekli navigation girdisi veya uygun ekran/graph sahibi kullanılacak; hesaplar/ekranlar arasında sızıntı ve yanlış duplicate gönderim önlenecek.
- Aynı mevcut talebin aktarılma/tüketilme akışı korunacak; yeniden oluşturma halinde otomatik yeni AI isteği gibi ürün davranışı eklenmeyecek.

Tasarım koruma şartı yalnız 31 için değil bütün maddeler için geçerlidir. İşleyişi etkileyen önceki doğruluk/ürün önerileri ayrıca netleştirilmeden uygulanmaz. Fazda ilgili gerekli testler yazılıp çalıştırılacak; genel cihazda performans/font scale/TalkBack doğrulaması son aşamada ayrıca yapılacak. Şimdi test/cihaz kontrolü yapılmış sayılmaz.

## Madde 32 — Gradle, bağımlılıklar, release ve dokümantasyon

Durum: Kullanıcı yedi öneriyi README'yi tüm refactor sonrasına bırakma şartıyla kabul etti. Üretim/build yapılandırması değiştirilmedi; tasarım ve mevcut işleyiş korunacak.

### 32.1 Tekrarlanan bağımlılıkları temizleme

- Room/Activity gibi farklı sürümlerle yinelenen tanımlar gerçek çözümlenen sürüm ve kullanım değerlendirilerek düzenlenecek; rastgele sürüm değiştirme yok.
- Sadece kaynakta yazan sürüm ile runtime'da çözümlenen sürüm aynı varsayılmayacak. Dependency doğrulaması uygulama aşamasında yapılacak; şimdi Gradle çalıştırılmadı.

### 32.2 Tutarlı sürüm yönetimi

- Uygun plugin/kütüphane sürümleri version catalog üzerinden yönetilecek. Firebase/Compose BoM kapsamındaki gereksiz ayrı sürümler kaldırılacak; gereken bilinçli istisnalar gerekçeleriyle korunacak.
- Amaç bütün sürümleri koşulsuz latest yapmak değil, mevcut işleyişle uyumlu tutarlı dependency grafiğidir.

### 32.3 Gerçek kullanım ve doğrudan dependency

- Doğrudan kullanılan API bağımlılıkları açık tanımlanacak. Kullanılmayan dependency ancak kaynak/Manifest/Gradle ve ilgili kullanım doğrulandıktan sonra kaldırılacak.
- Transitive dependency'ye tesadüfi yaslanma uygun yerde giderilecek; kütüphane sırf yeni veya şirketlerde popüler diye eklenmeyecek.

### 32.4 Ölçülü Gradle modernizasyonu

- Kabul edilen compilerOptions ve AI SDK değişiklikleri gerekli plugin/config uyumuyla birlikte yapılacak. Keyfi SDK/JVM target değişikliği yok.
- AGP/Kotlin büyük sürüm yükseltmesi, KSP veya multimodule yalnız gerçek ihtiyaç/uyumluluk gerekçesi ve küçük faz kapsamı varsa ele alınır; otomatik eklenmez.

### 32.5 Release ihtiyacının sınırı

- Kullanıcı projeyi yayımlamayacağını belirtti; Play yayını/release signing kurulumu bu refactorun zorunlu işi değildir. Gerektiğinde 23. maddedeki güvenli signing ilkeleri geçerlidir.
- R8/minify/ProGuard sırf şirket standardı diye etkinleştirilmeyecek; mevcut çalışan derleme davranışı korunacak. Gerçek ihtiyaç varsa etkisi ve doğrulama planı ayrıca netleşecek.

### 32.6 README'nin en son hazırlanması

- Kullanıcı özel olarak README'yi tüm refactor bittikten sonraya erteledi; ara fazlarda yeniden yazılmayacak. Nihai proje yapısına göre özenli kurulum/mimari/özellik açıklaması hazırlanacak.
- Araçlar, yerel yapılandırma, Firebase AI Logic ve gereken Console adımları güvenli örneklerle anlatılacak; gizli anahtar/parola/token eklenmeyecek.
- Olmayan özellik veya yapılmamış test/build başarısı iddia edilmeyecek; test sonuçları ancak gerçekten doğrulanmışsa yazılacak. Bu tur README değiştirilmedi.

### 32.7 Yardımcı yapılandırma tutarlılığı

- Wrapper çalıştırılabilirliği, repository/plugin tanımları ve ignore kuralları gerçek ihtiyaçla kontrol edilecek. Kullanıcı IDE dosyaları ve ilgisiz değişiklikler otomatik silinmeyecek.
- Build/uyumluluk doğrulamasının zamanı son uygulama faz planında belirlenir; güncel Test Kalitesi kararıyla ilgili test inceleme/yazma/çalıştırma her faza dahildir.

Tüm başlıklar kararlaştırılmadan üretim refactoru yok. README tüm refactor sonrası son dokümantasyon işidir; tasarım ve kullanıcı akışını değiştiren düzenlemeler ayrı somut onay gerektirir.

## Sonraki görüşme

Başlık görüşmeleri tamamlandı; beş fazlı ayrıntılı plan Altın Kurallar ve Test Kalitesi'nin ardından kayıtlıdır. Faz 1 temeli commit edildi; kullanıcı 5 Ekim 2026'da Faz 2'yi başlattı. Özgün teknik kapsam ve üç ek onay uygulandı/doğrulandı; takvim ayı kararı uygulandı ve negatif progress çizimi kullanıcı isteğiyle korundu. Faz 2 tamamlandı. Eski veri arşivlenir; hesap izolasyonu, gerçek çatışmada seçim ve kalıcı hesap para birimi uygulanmıştır. Eksiksiz devam için docs/refactor-phase-2-progress.md okunacak. Her faz öncesi iki kural bölümü ve madde ayrıntıları okunup güncel Git/dosya/test planı sunulacak. Açık ürün tercihleri ayrıca netleştirilecek; tasarım/işleyiş ayrıca onaylanan sınırlı istisnalar dışında değiştirilmeyecek. Her fazda gerekli testler incelenecek/yazılacak/uyarlanacak ve çalıştırılacak; kullanılmayan kod ve geçici loglar da temizlenecek. Genel son tarama/cihaz kontrolü/README kullanıcı ayrıca istediğinde yapılacak. Gerekli Console adımları ayrıca bildirilecek.

Bu belge her kabul edilen maddede mevcut durum, kesin karar, neden, kapsam, korunacak davranış, ertelenen ayrıntı ve uygulanmayan seçeneklerle güncellenecek.

## En güncel devam — Faz 2 uygulaması (5 Ekim 2026)

Son talimat özgün **8 veri/iş kuralları, 15–17 ve 24–26 + üç ek onay** birlikte uygulanmasıdır; üç karar önceki kalanların yerine geçmez. Ayrıntılı kaynak/karar/test/kalan iş kaydı **docs/refactor-phase-2-progress.md** içindedir. Üstteki görüşme/ilk-dilim notları tarihsel kanıttır, güncel durum değildir.

Üç ek karar uygulandı: logout'ta pending/yerel veri korunması ve A→B→A izolasyonu; gerçek örtüşen local/remote conflict'te iki sürüm + kullanıcı seçimi ve güncel remote sürüm kontrolü; ilk cihaz bölgesinden hesapta kalıcı currencyCode ve currency-scale Long alt birim. Eski geliştirme verisi için silme izni çıkarılmadı: tam v13 legacy arşivi korunur, sahibi belirsiz satırlar rastgele UID'ye atanmaz. Offline-first Altın Kural 10 kullanıcı talimatıyla kaldırıldı; mevcut mimari korunur, her faza zorunlu yeni offline işi eklenmez.

Özgün teknik kapsam: gerçek ApplicationScope/SupervisorJob/merkezi IO+Default; seri auth/session/startup/readiness, owner/generation guard ve work restore; atomik Room+outbox/tombstone, revision/mutation ack, server-transaction reconciliation, feature remote store ve conflict dialog; gerçek PK/ID, schema export ve veri koruyan 13→14 migration; ortak budget/FinancialSummary/completion/photo-work, currency/parsing/küsurat, java.time/Clock/exclusive periods/resume refresh. FirebaseSyncService ve boşa çıkan facade/module/type/API/import/XML/5 boş klasör kaldırıldı. PhotoMoveWorker persisted eski iş kimliği için bilinçli korunur, retirement Faz 3'tedir. Güvenli hata logları kalır; geçici debug çıktısı yok.

Son gerçek doğrulama: dört task **--rerun-tasks --offline**, BUILD SUCCESSFUL (51 saniye), **93/93 görev executed**; JVM XML **63/63**, debug/test APK başarılı; lint **0 error + 141 warning + 10 hint**, UnusedResources 0. Pixel_8a Android 16'da dört hedefli instrumentation sınıfı **14/14**, BUILD SUCCESSFUL (11 saniye). Genel UI/Compose/Firebase network/Google-login/multi-device manuel kabul veya sıfır uyarı garantisi değildir.

Önceki açık kararlar kapandı: Home “Bu ay” takvim ayını hesaplar; negatif bakiye progress çizimi kullanıcı kararıyla mevcut haliyle kalır. Faz 2'nin onaylı kapsamı son kapanış doğrulamasından sonra tamamlandı.

Faz 2 kapandı; Faz 3–5/README/genel son kabul başlamadı. Console/Rules/anahtar/Git history/GuideMate değişmedi; commit/push yapılmadı. Yeni faz ancak kullanıcı istediğinde başlatılır.

5 Ekim ek kullanıcı onayı uygulandı: tüm projede parametre/annotation/kod gövdesindeki gereksiz tam paket adları kısa ada, isim çakışmaları import alias'a çevrildi (19 Kotlin dosyası). Singleton temizliği **yalnız aynı bağımlılığın gereksiz çift scope'u** ile sınırlı: AuthRepositoryImpl sınıf scope'u kaldırıldı, AuthModule binding scope'u korundu; diğer singleton'lar değiştirilmedi. Son kaynakla 63/63 JVM, 14/14 hedefli cihaz, debug/test APK ve lint (0 error) başarılı. Ayrıntılar refactor-phase-2-progress.md içindedir; son takvim ayı onayı ve doğrulamasıyla Faz 2 kapanışı ayrıca yapılmıştır.

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
