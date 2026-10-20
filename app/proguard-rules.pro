# Правила R8 для release-сборки.
# Retrofit, OkHttp, Room и kotlinx.serialization приносят свои правила сами.
# Сюда добавляем только то, что ломается в release (проверяем assembleRelease каждую неделю).

# Читаемые стектрейсы при расшифровке через mapping.txt
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
