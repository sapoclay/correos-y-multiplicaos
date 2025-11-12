#!/bin/bash

# Script de lanzamiento para Correos Y Multiplicaos
# Este script limpia las variables de entorno problemáticas antes de ejecutar la aplicación

# Desactivar GTK_PATH que puede causar conflictos
unset GTK_PATH

# Desactivar LD_PRELOAD si causa problemas
unset LD_PRELOAD

# Limpiar LD_LIBRARY_PATH para evitar conflictos con snap
unset LD_LIBRARY_PATH

# Suprimir mensajes de GTK
export GTK_MODULES=""

# Obtener el directorio del script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Preferir JAR ensamblado (uber-jar). Si no existe, caer a classpath dinámico.
JAR_PATH="$DIR/target/gestor-correo-1.0-SNAPSHOT.jar"
# Forzar reconstrucción si el JAR está desactualizado respecto al código fuente o pom.xml
REBUILD_NEEDED=false
if [ -f "$JAR_PATH" ]; then
     if [ "$DIR/pom.xml" -nt "$JAR_PATH" ] || find "$DIR/src" -type f -newer "$JAR_PATH" | grep -q .; then
          echo "[run.sh] Detectado JAR desactualizado. Reconstruyendo con Maven..." >&2
          REBUILD_NEEDED=true
     fi
fi

if [ ! -f "$JAR_PATH" ] || [ "$REBUILD_NEEDED" = true ]; then
     echo "[run.sh] JAR no encontrado, construyendo con Maven (package)..." >&2
     mvn -q -DskipTests package
     if [ -f "$JAR_PATH" ]; then
          echo "[run.sh] JAR construido correctamente." >&2
          USE_JAR=true
     else
          echo "[run.sh] Construcción del JAR falló o no lo generó; usando classpath dinámico." >&2
          # Construir classpath dinámico: clases compiladas + dependencias de runtime
          PROJECT_CP=$(mvn -q -DskipTests -DincludeScope=runtime dependency:build-classpath -Dmdep.outputFile=/dev/stdout | grep -v "INFO" | tail -n 1)
          CLASSPATH="$DIR/target/classes:$PROJECT_CP"
          USE_JAR=false
     fi
else
     USE_JAR=true
fi

# Asegurar locale e Input Method adecuados para acentos en Linux
export LANG=${LANG:-es_ES.UTF-8}
export LC_ALL=${LC_ALL:-es_ES.UTF-8}
export GTK_IM_MODULE=${GTK_IM_MODULE:-ibus}
export QT_IM_MODULE=${QT_IM_MODULE:-ibus}
export XMODIFIERS=${XMODIFIERS:-@im=ibus}
export GDK_BACKEND=${GDK_BACKEND:-x11}

# Ejecutar la aplicación SIN limpiar el entorno (para mantener variables de Input Method del sistema)
JAVA_OPTS=(
           -Dfile.encoding=UTF-8 \
     -Dsun.jnu.encoding=UTF-8 \
     -Dawt.useSystemAAFontSettings=on \
     -Dswing.aatext=true \
     -Djava.awt.im.style=on-the-spot \
     -Dawt.toolkit=sun.awt.X11.XToolkit \
           -Dprism.text=Native \
)

if [ "$USE_JAR" = true ]; then
     exec java "${JAVA_OPTS[@]}" -jar "$JAR_PATH"
else
     exec java "${JAVA_OPTS[@]}" -cp "$CLASSPATH" com.gestorcorreo.Main
fi
