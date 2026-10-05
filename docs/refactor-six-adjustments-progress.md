# Altı ek düzenleme — kararlar ve eksiksiz devam kaydı

6 Ekim 2026. Kullanıcı son tartışılmış altı kararı **“tamam uygula”** ile yerel kod uygulaması için onayladı. Bu, Faz4/5'in kalanlarını, canlı Functions/Rules deploy'unu, Console/IAM/key/history/commit/push veya README yazımını otomatik başlatmaz. AltınKurallar/TestKalitesi tamamen okundu; kirli checkout'taki önceki Faz3/user değişiklikleri korunur.

## Güncel kabul edilen altı karar

1. GuideMate gibi tek gerçek MainActivity + tek FinanceApplication. Eski root launcher/PendingIntent component kimliği korunur; ikinci MainActivity ve gereksiz kalıtım yok.
2. NotificationActions küçük bildirim sahibi constval nesnesi; stable namespace+wire action değerleri ve legacyCANCEL→snooze korunur, receiverexportedfalse; Manifest gerçek dört action ile tutarlı.
3. Yalnız safUI state uygun Route/commonComposable sahibine; veri/form/search/inflight/validation VM'de. Config/navigation state lifetime korunur; formpassword SavedState'e konmaz. Alt state/callback işi sadece bu değişiklik için gerekli ölçüdedir; tam type-safe nav/Faz5 component refactoru otomatik değil.
4. Yedi combinedDAO/entity dosyası ayrılır; tümRoom persistence uygun owner local/dao,local/entity, sorguprojections local/model. Entity/SQltable/column/index/PK/wire değerleri değişmez; commonDatabase composition kalır. Testler test edilen üretim sınıfının paketini aynalar; model kullanan repository/migration testleri yanlışlıkla entity paketine taşınmaz, importları düzelir.
5. Beklenen hatalar dar ortak/feature typed hata; kullanıcı XML mesajları; raw e.message ve servermessage comparison kaldırılır; invariant require/check ve cancellation korunur. SDK/hata sunumu katmanları doğru kalır, megaErrorManager yok.
6. Repeated alan/wire sözleşmeleri owner-owned küçük Fields/collections; kapalıcommand/state/outcome gerektiğinde typed. Dev globalConstants veya herliteralconstant yok; editable financial field allowlist/completiondate/provenance ve eski storedwire değerleri korunur.

## Somut görev sahipliği / yapılanlar

- Ana ajan MainActivity birleştirdi: root com.ahmetkaragunlu.financeai.MainActivity artık ComponentActivity+AndroidEntryPoint, eski app/MainActivity silindi; onCreate/onResume/onNewIntent korunur. FinanceApplication değişmedi.
- Ana ajan NotificationActions ekledi, receiver/presenter/actionWorker bağlantılarını güncelledi; Manifest'e SNOOZE/DISMISS deklarasyonu ekledi. Runtime action values ve internalExplicit/immutable/owner guard değişmez.
- Ana ajan Room layout taşımasını yaptı: core/session/local, core/sync/local, core/media/local, fcm/data/local ve feature/schedule/data/local altında dao/entity; dörtfeature already separateDAO/entities aynı local alt konvansiyona alındı, transaction queryrows model'e. Class adları/schema/table/columns korunur; production+test imports güncellendi. Son import temizlik/DBschema karşılaştırması/test kanıtı henüz tamamlanmadı.
- UI-state worker **Darwin /01a10dec-3895-7502-88d1-7ec3d3f887f7**: ilgili auth/home/mainnav/transaction/location presentation dosyaları ve tests; budget/stateoutbox/domain/runtime dışı. Main typederror değişiklikleriyle aynıVM'de çakışmamak için worker tamamlanınca main hata mapping'i yapacak. Gradle worker'da çalışmaz.
- Server worker **Aquinas /01a10dec-36c6-7883-b5b5-d97f50952ba0**: yalnız functions/index/src ve Node tests; typedcodeError ve field/closedvaluecontracts. Wire/behavior değişmez; deploy yok.
- Ana ajan Android typed error ve sözleşme alanlarını tamamladı: Firebase/Firestore beklenen hataları dar typed sınıflara çevrildi, kullanıcı metinleri XML sunum katmanına alındı, ham `Throwable.message` ve sunucu diagnostic-message karşılaştırmaları kaldırıldı. Tekrarlanan finansal, fotoğraf, Firestore collection, sync ve FCM alanları küçük owner-owned sözleşme nesnelerine bağlandı; command/state/outcome değerleri typed boundary ile okunuyor. Local code değişiklikleri canlıya yayımlanmadı.
- Son statik kontrolde tek `com.ahmetkaragunlu.financeai.MainActivity`, tek `FinanceApplication`, Manifest/PendingIntent action değerleri ve yeni Room FQN'leri doğrulandı. Eski moved FQN/import referansı bulunmadı; `git diff --check` temiz.

## Sonraki güvenli adımlar

1. Worker durumlarını oku; zaten bitmiş MainActivity/NotificationActions/Room taşımasını yeniden yapma. UI-worker sahipliğindeki VM dosyalarına typederror değişikliğini worker tamamlamadan üstten yazma.
2. ~~Android typederror/SDK mapping/XML sunumu + repeatedfield/command/state sözleşmelerini bitir.~~ Tamamlandı. Exception text ile karar verme/raw message gösterme kalmadı; invariant ve cancellation korunuyor.
3. ~~Başarılı worker değişikliklerini ana ajan incele; test/state lifetime/Screen görünümünü kontrol et.~~ Tamamlandı. UI state sahipliği korunarak incelendi; yalnız gerçek sahiplik ve sözleşme tekrarları düzenlendi.
4. Tek Gradle sahibi ana ajan; Java21, `testDebugUnitTest assembleDebug assembleDebugAndroidTest compileReleaseKotlin lintDebug --rerun-tasks` final source üzerinde çalıştırılmalı. Bu checkout'ta wrapper dağıtımı/cache kilidi, network ve sandbox socket kısıtları nedeniyle Android Gradle doğrulaması henüz çalışmadı; bu nedenle build/lint/test sonucu başarılı sayılmıyor.
5. ~~DBv15 identity/hash/table/index/column schema değişmediğini doğrula; test-package mirrors/DI ownership/domain SDK bağımlılıkları/oldFQN/oldactionrefs/strings kontratı son tarama.~~ Statik karşılaştırma ve FQN taraması tamamlandı; Room v15 şeması ve identity hash kaydı korunuyor. Android Gradle çalışmadığı için generated Room/compiler doğrulaması ayrıca bekliyor.
6. Node exact 22 tabanlı functions testleri çalıştırıldı: **59 geçti, 0 başarısız, 0 atlandı**. Console/deploy/GoogleAuth/userdata/newtestaccounts kapsam dışı bırakıldı.

## Doğrulama durumu

Bu yeni altı düzenlemenin Android final Gradle/test/lint turu ortam kısıtı nedeniyle henüz çalışmadı; Android kaynak geçti varsayılmıyor. Functions doğrulaması güncel kaynakta Node v22.23.3 ile 59/59 geçti. Kullanıcı/device/two-device manual kabul bu refaktör doğrulaması olarak sayılmadı. Deploy, Rules, Console, IAM, anahtar geçmişi, README ve commit/push bu çalışma kapsamına alınmadı.
