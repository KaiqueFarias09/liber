cmake_minimum_required(VERSION 3.22.1)
project(cr3engine)

# All sources live inside this directory (self-contained build).
set(CR3_CPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}")

# ── Thirdparty source-version variables ─────────────────────────────────────
include(${CR3_CPP_DIR}/repo_srcdirs.cmake)

# ── Include paths ────────────────────────────────────────────────────────────
include_directories(${CR3_CPP_DIR}/crengine/include)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_LIBPNG_SRCDIR})
include_directories(${CR3_CPP_DIR}/thirdparty_libs/libpng/lib)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_FREETYPE_SRCDIR})
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_FREETYPE_SRCDIR}/include)
include_directories(${CR3_CPP_DIR}/thirdparty_libs/freetype)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_HARFBUZZ_SRCDIR}/src)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_LIBJPEG_SRCDIR})
include_directories(${CR3_CPP_DIR}/thirdparty_unman/antiword)
include_directories(${CR3_CPP_DIR}/thirdparty_unman/chmlib/src)
include_directories(${CR3_CPP_DIR}/thirdparty_unman/nanosvg/src)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_FRIBIDI_SRCDIR}/lib)
include_directories(${CR3_CPP_DIR}/thirdparty_libs/fribidi/lib)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_LIBUNIBREAK_SRCDIR}/src)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_ZSTD_SRCDIR}/lib)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_UTF8PROC_SRCDIR})

# ── Compile-time definitions (mirror coolreader's Android.mk) ───────────────
add_definitions(-DLINUX=1 -D_LINUX=1)
add_definitions(-DFT_CONFIG_MODULES_H=<android/config/ftmodule.h>
                -DFT_CONFIG_OPTIONS_H=<android/config/ftoption.h>)
add_definitions(-DDOC_DATA_COMPRESSION_LEVEL=1 -DDOC_BUFFER_SIZE=0x1000000)
add_definitions(-DENABLE_CACHE_FILE_CONTENTS_VALIDATION=1)
add_definitions(-DLDOM_USE_OWN_MEM_MAN=0)
add_definitions(-DCR3_ANTIWORD_PATCH=1 -DENABLE_ANTIWORD=1)
add_definitions(-DLVLONG_FILE_SUPPORT=1)
add_definitions(-DMAX_IMAGE_SCALE_MUL=2)
add_definitions(-DUSE_NANOSVG=1)
add_definitions(-DBUNDLED_FRIBIDI=1)
add_definitions(-DKO_LIBUNIBREAK_PATCH=1)
add_definitions(-DUSE_UTF8PROC=1)

set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -funwind-tables")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -funwind-tables -std=c++17 -frtti -fexceptions")

# ── CREngine sources ─────────────────────────────────────────────────────────
set(CRENGINE_SRC
    ${CR3_CPP_DIR}/crengine/src/cp_stats.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstring.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstring8collection.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstring32collection.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstring32hashedcollection.cpp
    ${CR3_CPP_DIR}/crengine/src/crlog.cpp
    ${CR3_CPP_DIR}/crengine/src/serialbuf.cpp
    ${CR3_CPP_DIR}/crengine/src/props.cpp
    ${CR3_CPP_DIR}/crengine/src/lstridmap.cpp
    ${CR3_CPP_DIR}/crengine/src/rtfimp.cpp
    ${CR3_CPP_DIR}/crengine/src/lvmemman.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstyles.cpp
    ${CR3_CPP_DIR}/crengine/src/crtxtenc.cpp
    ${CR3_CPP_DIR}/crengine/src/lvtinydom.cpp
    ${CR3_CPP_DIR}/crengine/src/chmfmt.cpp
    ${CR3_CPP_DIR}/crengine/src/epubfmt.cpp
    ${CR3_CPP_DIR}/crengine/src/pdbfmt.cpp
    ${CR3_CPP_DIR}/crengine/src/wordfmt.cpp
    ${CR3_CPP_DIR}/crengine/src/lvopc.cpp
    ${CR3_CPP_DIR}/crengine/src/docxfmt.cpp
    ${CR3_CPP_DIR}/crengine/src/fb3fmt.cpp
    ${CR3_CPP_DIR}/crengine/src/odtfmt.cpp
    ${CR3_CPP_DIR}/crengine/src/odxutil.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstsheet.cpp
    ${CR3_CPP_DIR}/crengine/src/crtest.cpp
    ${CR3_CPP_DIR}/crengine/src/lvbmpbuf.cpp
    ${CR3_CPP_DIR}/crengine/src/hyphman.cpp
    ${CR3_CPP_DIR}/crengine/src/crskin.cpp
    ${CR3_CPP_DIR}/crengine/src/lvdocview.cpp
    ${CR3_CPP_DIR}/crengine/src/lvpagesplitter.cpp
    ${CR3_CPP_DIR}/crengine/src/lvtextfm.cpp
    ${CR3_CPP_DIR}/crengine/src/lvrend.cpp
    ${CR3_CPP_DIR}/crengine/src/wolutil.cpp
    ${CR3_CPP_DIR}/crengine/src/crconcurrent.cpp
    ${CR3_CPP_DIR}/crengine/src/mathml.cpp
    ${CR3_CPP_DIR}/crengine/src/hist.cpp
    ${CR3_CPP_DIR}/crengine/src/xxhash.c
    ${CR3_CPP_DIR}/crengine/src/textlang.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvcacheableobject.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvimagesource.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/crninepatchdecoder.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvjpegimagesource.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvpngimagesource.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvxpmimagesource.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvgifimagesource.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvgifframe.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/clzwdecoder.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvsvgimagesource.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvstretchimgsource.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvcolortransformimgsource.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvalphatransformimgsource.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvunpackedimgsource.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvdrawbufimgsource.cpp
    ${CR3_CPP_DIR}/crengine/src/lvimg/lvimg.cpp
    ${CR3_CPP_DIR}/crengine/src/lvfont/lvfnt.cpp
    ${CR3_CPP_DIR}/crengine/src/lvfont/lvembeddedfont.cpp
    ${CR3_CPP_DIR}/crengine/src/lvfont/lvfont.cpp
    ${CR3_CPP_DIR}/crengine/src/lvfont/lvfntman.cpp
    ${CR3_CPP_DIR}/crengine/src/lvfont/lvfontglyphcache.cpp
    ${CR3_CPP_DIR}/crengine/src/lvfont/lvfontboldtransform.cpp
    ${CR3_CPP_DIR}/crengine/src/lvfont/lvfontcache.cpp
    ${CR3_CPP_DIR}/crengine/src/lvfont/lvfontdef.cpp
    ${CR3_CPP_DIR}/crengine/src/lvfont/lvfreetypeface.cpp
    ${CR3_CPP_DIR}/crengine/src/lvfont/lvfreetypefontman.cpp
    ${CR3_CPP_DIR}/crengine/src/lvdrawbuf/lvbasedrawbuf.cpp
    ${CR3_CPP_DIR}/crengine/src/lvdrawbuf/lvgraydrawbuf.cpp
    ${CR3_CPP_DIR}/crengine/src/lvdrawbuf/lvcolordrawbuf.cpp
    ${CR3_CPP_DIR}/crengine/src/lvdrawbuf/lvinkmeasurementdrawbuf.cpp
    ${CR3_CPP_DIR}/crengine/src/lvdrawbuf/lvimagescaleddrawcallback.cpp
    ${CR3_CPP_DIR}/crengine/src/lvdrawbuf/lvdrawbuf_utils.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvdefstreambuffer.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvfilemappedstream.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvnamedstream.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvfilestream.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvdirectorycontainer.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvcachedstream.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvzipdecodestream.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvziparc.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvrararc.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvmemorystream.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvtcrstream.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvblockwritestream.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvstream.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvbase64stream.cpp
    ${CR3_CPP_DIR}/crengine/src/lvstream/lvstreamutils.cpp
    ${CR3_CPP_DIR}/crengine/src/lvxml/lvfileparserbase.cpp
    ${CR3_CPP_DIR}/crengine/src/lvxml/lvtextfilebase.cpp
    ${CR3_CPP_DIR}/crengine/src/lvxml/lvtextparser.cpp
    ${CR3_CPP_DIR}/crengine/src/lvxml/lvtextlinequeue.cpp
    ${CR3_CPP_DIR}/crengine/src/lvxml/pmltextimport.cpp
    ${CR3_CPP_DIR}/crengine/src/lvxml/lvtextrobustparser.cpp
    ${CR3_CPP_DIR}/crengine/src/lvxml/lvtextbookmarkparser.cpp
    ${CR3_CPP_DIR}/crengine/src/lvxml/lvxmlparser.cpp
    ${CR3_CPP_DIR}/crengine/src/lvxml/lvhtmlparser.cpp
    ${CR3_CPP_DIR}/crengine/src/lvxml/fb2coverpageparsercallback.cpp
    ${CR3_CPP_DIR}/crengine/src/lvxml/lvxmlutils.cpp
    ${CR3_CPP_DIR}/crengine/src/locale_data/fc-lang-data.c
    ${CR3_CPP_DIR}/crengine/src/locale_data/crlocaledata.cpp
)

# ── CoolReader JNI bridge ────────────────────────────────────────────────────
set(JNI_SRC
    ${CR3_CPP_DIR}/jni/cr3engine.cpp
    ${CR3_CPP_DIR}/jni/cr3java.cpp
    ${CR3_CPP_DIR}/jni/docview.cpp
)

# ── CoffeeCatch crash handler (arm/x86 only) ────────────────────────────────
set(COFFEECATCH_SRC
    ${CR3_CPP_DIR}/jni/coffeecatch/coffeecatch.c
    ${CR3_CPP_DIR}/jni/coffeecatch/coffeejni.c
)

set(ALL_SRC ${JNI_SRC} ${CRENGINE_SRC})

if("${ANDROID_ABI}" STREQUAL "armeabi-v7a" OR
   "${ANDROID_ABI}" STREQUAL "arm64-v8a"   OR
   "${ANDROID_ABI}" STREQUAL "x86")
    list(APPEND ALL_SRC ${COFFEECATCH_SRC})
endif()

# ── Thirdparty static libraries ──────────────────────────────────────────────
add_subdirectory(${CR3_CPP_DIR}/thirdparty_libs ${CMAKE_CURRENT_BINARY_DIR}/thirdparty_libs)

# ── Shared library ───────────────────────────────────────────────────────────
find_library(log-lib log)

add_library(cr3engine SHARED ${ALL_SRC})

target_link_libraries(cr3engine
    local_png
    local_jpeg
    local_freetype
    local_harfbuzz
    local_chmlib
    local_antiword
    local_fribidi
    local_unibreak
    local_qimagescale
    local_zstd
    local_utf8proc
    ${log-lib}
    -lm -lz -ldl
)
