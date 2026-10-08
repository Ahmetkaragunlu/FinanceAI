# FinanceAI — Değişim Listesi

## Altın Kurallar

**Zorunlu başlangıç:** Her faz veya düzenleme öncesinde Altın Kurallar ve Test Kalitesi bölümlerinin tamamı kesinlikle okunacak; güncel kod ve Git durumu incelenerek somut dosya ve test planı hazırlanacak. Önceden okunmuş olması yeni çalışma için yeterli değildir.

**Zorunlu değerlendirme:** SOLID, bağımlılıkların yönü ve DI, test edilebilirlik, genişletilebilirlik, okunabilirlik, isimlendirme, paket yapısı, katman doğruluğu, ekranların bulunduğu paket ve katmanların konum doğruluğu, kod tekrarı, gerçek ortak yapılar ve uygun merkezi yönetim birlikte dikkate alınacak. Bu ilkeler dokunulan bütün kod için geçerlidir. Over-engineering kesinlikle yok; yalnız gerçek ihtiyaç için yapı eklenir. **Mevcut işleyiş ve tasarım değiştirilmeyecek.** Ayrı davranış/tasarım değişikliği gerektiren bulgu refactorun dışında tutulup somut etkisi kullanıcıya sunulacak; ayrıca onay verilmedikçe uygulanmayacak.

1. **Her değişikliğin somut gerekçesi olacak.** Okunabilirlik, sorumluluk ayrımı, bağımlılık, gerçek tekrar veya test edilebilirlik ihtiyacını çözmeyen yapı eklenmeyecek. Kullanmak için teknoloji/katman kullanılmayacak.
2. **Doğru mevcut kod korunacak.** Her sınıf yeniden yazılmayacak; her metoda use case, her yardımcıya interface, BaseViewModel/BaseRepository ağı veya gereksiz framework eklenmeyecek.
3. **Parçalama sorumluluğa göre yapılacak.** Satır/parametre sayısı tek başına kusur değildir. Ayrılan parçalar anlamlı isim ve açık sahiplik taşıyacak; sırf dosyayı küçültmek için anlaşılmaz parçalama yapılmayacak.
4. **Ortaklaştırma anlam üzerinden yapılacak.** Aynı iş kuralı tek sahipte yönetilecek; yalnız görünüşü benzer ekranlar çok seçenekli dev ortak bileşene zorlanmayacak. Gerçek feature farklılıkları korunacak.
5. **Tutarlılık bütün uygulamada sağlanacak.** Eşdeğer durumlarda isimlendirme, state, callback, hata ve coroutine yaklaşımı tutarlı olacak. Farklı sorumluluk/yaşam süresi gerektiren bilinçli farklılıklar korunacak; örnek by/value veya login referansı ile sınırlı tarama yapılmayacak. Gereksiz yinelenen singleton tanımları ve isim çakışması gerektirmeyen tam nitelikli sınıf kullanımları önlenecek; uygun importlar kullanılacak.
6. **Tasarım ve mevcut işleyiş korunacak.** Görünüm, renk, tipografi, yerleşim ve kullanıcı akışları değiştirilmeden refactor yapılacak. Kod taşıma/SDK geçişi mevcut özellikleri bozmayacak. Tasarım veya davranış etkileyen düzeltmeler somut etkileriyle ayrıca kullanıcı onayına sunulacak; genel refactor onayı bu sınırı aşma yetkisi değildir. Önceden ayrıca onaylanmış davranışlar korunacak.
7. **Kalite değerlendirmesi kanıta dayanacak.** Profesyonellik dosya/katman sayısıyla değil anlaşılır sahiplik, doğru bağımlılıklar, bakım kolaylığı ve gerçekten yapılmış doğrulamalarla değerlendirilecek. Yapılmamış test/build/lint/runtime kontrolü başarılı sayılmayacak; evrensel şirket standardı veya sıfır hata garantisi verilmeyecek.
8. **Her fazda gerçek kullanılmayan kod temizlenecek.** Değişikliklerle boşa çıkan ve gerçekten hiçbir kullanım/giriş noktası olmayan dosya, ekran, fonksiyon, sınıf, import, kullanılmayan XML kaynağı ve kod kaldırılacak. Boş eski paketler bırakılmayacak. Yalnız metinsel referans yokluğu silme kanıtı değildir: Manifest, Hilt/KSP, Room, reflection, navigation/deep link, persisted Worker kimlikleri ve dış sözleşmeler kontrol edilecek. Bilinçli bırakılmış uyumluluk parçaları veya açık bir sonraki iş bu kapsama alınmaz; gerekçesi açık olacak. Kaynak dizininde boş klasörlerin varlığı çalışma zamanı paketi yaratmaz.
9. **Her fazda geçici konsol/debug logları temizlenecek.** Yalnız geliştirme sırasında konsolda değer/akış görmek için eklenmiş println/print ve debug logları kaldırılacak. Hata teşhisi, operasyon veya güvenli davranış takibi için gerçekten gerekli loglar korunabilir; token, anahtar, parola, kullanıcı finansal verisi, e-posta veya kişisel dosya yolu gibi hassas içerik yazılmayacak. Bütün loglar körlemesine silinmez; kalanların amacı açık olacak. Bu temizlik tasarım ve mevcut işleyişi değiştirmeyecek.

Mevcut Edit* bileşenlerini yeniden adlandırmama istisnası, uygulama öncesi dosya planı sunma ve README'yi en son hazırlama kararları geçerlidir.

### Ürün ve davranış kararlarının sınırı

- **Refactor ile davranış düzeltmesi ayrılacak.** Paket taşıma, sorumluluk parçalama, DI ve tekrar azaltma mevcut sonucu koruyacak. Kullanıcıya görünen sonucu değiştiren düzeltmenin somut etkisi ayrıca açıklanıp onay alınacak.
- **Finansal kurallar kendiliğinden değişmeyecek.** Para birimi, yüzde tabanı, dönem anlamı ve yuvarlama mevcut kullanım üzerinden netleştirilecek; eski veriler sessizce yeni anlamla yorumlanmayacak.
- **Bildirim ürün politikası ayrı karar olacak.** Tekrar, erteleme, missed reminder ve süresi geçen planı silme/koruma refactor bahanesiyle değişmeyecek. Teknik ayrıştırma ürün tercihini otomatik seçmez.
- **Veri koruma tercihleri açık uygulanacak.** Hesap izolasyonu ve logout'ta pending veri koruma korunacak. Sahibi belirsiz eski kayıtların migration'ı, backup kapsamı ve çok cihazlı conflict önceliği ayrıca kararlaştırılacak.
- **AI işlevleri korunacak.** Model sohbet hafızası ekleme, finansal veri kapsamını değiştirme, history silme veya yeni kullanıcı limitleri otomatik eklenmeyecek. Kota/girdi sınırının mevcut akışa etkisi gerekiyorsa ayrıca açıklanacak.
- **Tasarım etkisi ayrı onay gerektirecek.** Renk, layout, yazı boyutu, görünen hata sunumu veya grafik değişimi önce somut farkıyla sunulacak. A11y/responsive gerekçesi onaysız görsel değişiklik izni değildir.
- **Açık kararlar kontrol edilecek.** Korunacak davranış, önerilen fark ve gereken kullanıcı onayı çalışma öncesinde belirtilecek. Karar gerektirmeyen refactor ilerleyebilir; belirsiz ürün davranışı varsayımla değiştirilmez.

Temel kural: Kodun iç yapısını düzenleme onayı, ürün tasarımını veya çalışma mantığını değiştirme onayı değildir. Netleştirilmemiş tercihler tamamlanmış karar gibi raporlanmayacak.

## Test Kalitesi

- Her fazda değişen iş kuralları, hata/iptal yolları, veri bütünlüğü ve korunması gereken davranışlar için riskle orantılı testler yazılıp çalıştırılacak; etkilenen mevcut testler incelenip gerektiğinde uyarlanacak.
- Sırf test sayısı veya coverage oranını artırmak için test yazılmayacak. Basit getter, sabit değer ve framework davranışını tekrar doğrulayan düşük değerli testlerden kaçınılacak. Bu ilke TextField veya başka tek örnekle sınırlı değildir; bütün testlere uygulanır. Önemli risk testleri gereksiz denilerek atlanmayacak.
- Testler anlamlı senaryoları ve gözlemlenebilir davranışı doğrulayacak; iç implementasyona gereksiz bağlanıp her refactorda kırılmayacak. Saf iş kuralı/VM testi yeterliyse gereksiz UI testi yazılmayacak; gerçek UI entegrasyon riski varsa uygun UI testi kullanılacak.
- Test edilen üretim sınıfının feature-first paketi aynalanacak: JVM testleri src/test, cihaz/Compose testleri src/androidTest altında aynı paket/sahiplik düzeninde olacak. Core/app sınıfları da kendi üretim paketini aynalayacak; her test zorla feature altına konmayacak.
- Gerçekten birden fazla testte kullanılan fixture/fake yardımcıları uygun test destek paketinde tutulabilir; tek feature'a zorla yerleştirilmeyecek veya gereksiz kopyalanmayacak. Yalnız bir testin kullandığı yardımcı mümkünse o testin yakınında kalacak.
- Tüm refactor sonunda kullanıcı son aşamayı istediğinde uygulamanın test kapsamı yeniden taranacak ve eksikler tamamlanacak. Faz testleri genel regresyon ve cihaz kontrollerinin yerine geçmeyecek.
- Her faz sonunda ilgili test sonuçları açıkça raporlanacak: ne çalıştırıldı, ne geçti/başarısız oldu ve ne çalıştırılamadı belirtilecek. Çalıştırılamayan veya yalnız yazılmış test başarılı sayılmayacak; JVM ve cihaz testlerinin sonuçları ayrılacak.

Profesyonel seviye çok test değil; doğru riski yakalayan, okunabilir ve sürdürülebilir testlerdir. Test edilebilirlik gerekçesiyle üretim koduna gereksiz soyutlama veya test modunda farklı davranış eklenmeyecek. Mevcut tasarım/işleyişi koruma şartı test planında da esas alınacak.
