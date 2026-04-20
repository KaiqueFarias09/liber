cmake_minimum_required(VERSION 3.22.1)
project(cr3engine)

# ── crengine-ng location ──────────────────────────────────────────────────────
# crengine-ng is bundled as a git submodule at third_party/crengine-ng.
# Run `git submodule update --init` after cloning to populate it.
# Override CRENGINE_NG_DIR on the command line if needed.
set(CRENGINE_NG_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/crengine-ng/crengine"
    CACHE PATH "Path to the crengine-ng/crengine source directory")
set(CRENGINE_NG_THIRDPARTY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/crengine-ng/thirdparty"
    CACHE PATH "Path to the crengine-ng/thirdparty directory")

if(NOT EXISTS "${CRENGINE_NG_DIR}/include/crengine.h")
    message(FATAL_ERROR
        "crengine-ng submodule not populated at '${CRENGINE_NG_DIR}'.\n"
        "Run: git submodule update --init --recursive")
endif()

# All non-engine sources live inside this directory.
set(CR3_CPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}")

# ── Thirdparty source-version variables ──────────────────────────────────────
include(${CR3_CPP_DIR}/repo_srcdirs.cmake)

# ── crengine-ng feature flags (consumed by crengine-ng-config.h.cmake) ───────
set(DOC_DATA_COMPRESSION_LEVEL 1)
set(DOC_BUFFER_SIZE            0x1000000)
set(MAX_IMAGE_SCALE_MUL        2)
set(GRAY_BACKBUFFER_BITS       4)
set(USE_LOCALE_DATA            1)
set(COLOR_BACKBUFFER           1)
set(LDOM_USE_OWN_MEM_MAN       0)
set(USE_ZLIB                   1)
set(USE_LIBPNG                 1)
set(USE_LIBJPEG                1)
set(USE_LIBWEBP                0)   # not bundled
set(USE_GIF                    1)
set(USE_LIBJXL                 0)   # not bundled
set(USE_FREETYPE               1)
set(USE_HARFBUZZ               1)
set(USE_LIBUNIBREAK            1)
set(USE_FRIBIDI                1)
set(USE_ZSTD                   1)
set(USE_FONTCONFIG             0)   # not needed on Android
set(USE_UTF8PROC               1)
set(USE_CMARK_GFM              0)
set(USE_NANOSVG                1)
set(USE_CHM                    1)
set(USE_ANTIWORD               1)
set(USE_SHASUM                 0)
set(USE_MD4C                   1)

configure_file(
    "${CRENGINE_NG_DIR}/crengine-ng-config.h.cmake"
    "${CMAKE_CURRENT_BINARY_DIR}/crengine-ng-config.h"
)

# ── Include paths ─────────────────────────────────────────────────────────────
include_directories(${CMAKE_CURRENT_BINARY_DIR})                    # crengine-ng-config.h
include_directories(${CRENGINE_NG_DIR}/include)                     # public crengine-ng headers
include_directories(${CRENGINE_NG_DIR}/src)                         # private format headers
include_directories(${CRENGINE_NG_DIR}/src/lvxml)                   # private XML helpers
include_directories(${CRENGINE_NG_THIRDPARTY_DIR}/nanosvg/src)      # nanosvg
include_directories(${CRENGINE_NG_THIRDPARTY_DIR}/chmlib/src)       # chmlib
include_directories(${CRENGINE_NG_THIRDPARTY_DIR}/antiword)         # antiword
include_directories(${CRENGINE_NG_THIRDPARTY_DIR}/qimagescale)      # qimagescale
include_directories(${CRENGINE_NG_THIRDPARTY_DIR}/md4c/src)         # md4c-html.h

# Bundled external thirdparty (freetype, harfbuzz, libpng, etc.)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_LIBPNG_SRCDIR})
include_directories(${CR3_CPP_DIR}/thirdparty_libs/libpng/lib)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_FREETYPE_SRCDIR})
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_FREETYPE_SRCDIR}/include)
include_directories(${CR3_CPP_DIR}/thirdparty_libs/freetype)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_HARFBUZZ_SRCDIR}/src)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_LIBJPEG_SRCDIR})
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_FRIBIDI_SRCDIR}/lib)
include_directories(${CR3_CPP_DIR}/thirdparty_libs/fribidi/lib)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_LIBUNIBREAK_SRCDIR}/src)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_ZSTD_SRCDIR}/lib)
include_directories(${CR3_CPP_DIR}/thirdparty/${REPO_UTF8PROC_SRCDIR})

# ── Compile-time definitions ──────────────────────────────────────────────────
add_definitions(-DLINUX=1 -D_LINUX=1 -DCR_EMULATE_GETTEXT=1)
add_definitions(-DFT_CONFIG_MODULES_H=<android/config/ftmodule.h>
                -DFT_CONFIG_OPTIONS_H=<android/config/ftoption.h>)
add_definitions(-DENABLE_CACHE_FILE_CONTENTS_VALIDATION=1)
add_definitions(-DLDOM_USE_OWN_MEM_MAN=0)
add_definitions(-DCR3_ANTIWORD_PATCH=1 -DENABLE_ANTIWORD=1)
add_definitions(-DLVLONG_FILE_SUPPORT=1)
add_definitions(-DMAX_IMAGE_SCALE_MUL=2)
add_definitions(-DUSE_NANOSVG=1)
add_definitions(-DBUNDLED_FRIBIDI=1)
add_definitions(-DKO_LIBUNIBREAK_PATCH=1)
add_definitions(-DUSE_UTF8PROC=1)
add_definitions(-DUSE_MD4C=1)
add_definitions(-DDOC_DATA_COMPRESSION_LEVEL=1)
add_definitions(-DDOC_BUFFER_SIZE=0x1000000)

set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -funwind-tables")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -funwind-tables -std=c++17 -frtti -fexceptions")

# ── crengine-ng internal thirdparty (OBJECT libraries) ───────────────────────
add_subdirectory(${CRENGINE_NG_THIRDPARTY_DIR}/antiword
                 ${CMAKE_CURRENT_BINARY_DIR}/thirdparty/antiword)
target_compile_definitions(cre_antiword PRIVATE -DCR3_ANTIWORD_PATCH=1)

add_subdirectory(${CRENGINE_NG_THIRDPARTY_DIR}/chmlib
                 ${CMAKE_CURRENT_BINARY_DIR}/thirdparty/chmlib)

add_subdirectory(${CRENGINE_NG_THIRDPARTY_DIR}/qimagescale
                 ${CMAKE_CURRENT_BINARY_DIR}/thirdparty/qimagescale)

add_subdirectory(${CRENGINE_NG_THIRDPARTY_DIR}/md4c
                 ${CMAKE_CURRENT_BINARY_DIR}/thirdparty/md4c)

# ── crengine-ng engine sources ────────────────────────────────────────────────
set(CRENGINE_NG_SRC ${CRENGINE_NG_DIR}/src)

set(CRENGINE_SRC
    ${CRENGINE_NG_SRC}/cp_stats.cpp
    ${CRENGINE_NG_SRC}/lvstring.cpp
    ${CRENGINE_NG_SRC}/lvstring8collection.cpp
    ${CRENGINE_NG_SRC}/lvstring32collection.cpp
    ${CRENGINE_NG_SRC}/lvstring32hashedcollection.cpp
    ${CRENGINE_NG_SRC}/lvserialbuf.cpp
    ${CRENGINE_NG_SRC}/crlog.cpp
    ${CRENGINE_NG_SRC}/crprops.cpp
    ${CRENGINE_NG_SRC}/rtfimp.cpp
    ${CRENGINE_NG_SRC}/cri18n.cpp
    ${CRENGINE_NG_SRC}/lvmemman.cpp
    ${CRENGINE_NG_SRC}/lvstyles.cpp
    ${CRENGINE_NG_SRC}/crtxtenc.cpp
    ${CRENGINE_NG_SRC}/lvstsheet.cpp
    ${CRENGINE_NG_SRC}/textlang.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvdefstreambuffer.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvfilemappedstream.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvnamedstream.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvfilestream.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvdirectorycontainer.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvcachedstream.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvzipdecodestream.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvziparc.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvmemorystream.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvtcrstream.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvblockwritestream.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvstream.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvbase64stream.cpp
    ${CRENGINE_NG_SRC}/lvstream/lvstreamutils.cpp
    ${CRENGINE_NG_SRC}/lvxml/lvfileparserbase.cpp
    ${CRENGINE_NG_SRC}/lvxml/lvtextfilebase.cpp
    ${CRENGINE_NG_SRC}/lvxml/lvtextparser.cpp
    ${CRENGINE_NG_SRC}/lvxml/lvtextlinequeue.cpp
    ${CRENGINE_NG_SRC}/lvxml/pmltextimport.cpp
    ${CRENGINE_NG_SRC}/lvxml/lvtextbookmarkparser.cpp
    ${CRENGINE_NG_SRC}/lvxml/lvxmlparser.cpp
    ${CRENGINE_NG_SRC}/lvxml/lvhtmlparser.cpp
    ${CRENGINE_NG_SRC}/lvxml/fb2coverpageparsercallback.cpp
    ${CRENGINE_NG_SRC}/lvxml/lvxmlutils.cpp
    ${CRENGINE_NG_SRC}/crhyphman.cpp
    ${CRENGINE_NG_SRC}/crskin.cpp
    ${CRENGINE_NG_SRC}/lvdocview.cpp
    ${CRENGINE_NG_SRC}/lvpagesplitter.cpp
    ${CRENGINE_NG_SRC}/lvtextfm.cpp
    ${CRENGINE_NG_SRC}/lvrend.cpp
    ${CRENGINE_NG_SRC}/wolutil.cpp
    ${CRENGINE_NG_SRC}/crhist.cpp
    ${CRENGINE_NG_SRC}/chmfmt.cpp
    ${CRENGINE_NG_SRC}/epubfmt.cpp
    ${CRENGINE_NG_SRC}/pdbfmt.cpp
    ${CRENGINE_NG_SRC}/wordfmt.cpp
    ${CRENGINE_NG_SRC}/lvopc.cpp
    ${CRENGINE_NG_SRC}/docxfmt.cpp
    ${CRENGINE_NG_SRC}/fb3fmt.cpp
    ${CRENGINE_NG_SRC}/odtfmt.cpp
    ${CRENGINE_NG_SRC}/odxutil.cpp
    ${CRENGINE_NG_SRC}/crconcurrent.cpp
    ${CRENGINE_NG_SRC}/mathml.cpp
    ${CRENGINE_NG_SRC}/mdfmt.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvcacheableobject.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvimagesource.cpp
    ${CRENGINE_NG_SRC}/lvimg/crninepatchdecoder.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvjpegimagesource.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvpngimagesource.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvxpmimagesource.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvgifimagesource.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvgifframe.cpp
    ${CRENGINE_NG_SRC}/lvimg/clzwdecoder.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvsvgimagesource.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvstretchimgsource.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvcolortransformimgsource.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvalphatransformimgsource.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvunpackedimgsource.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvdrawbufimgsource.cpp
    ${CRENGINE_NG_SRC}/lvimg/lvimg.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvfnt.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvembeddedfont.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvfont.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvfntman.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvfontglyphcache.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvfontboldtransform.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvfontcache.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvfontdef.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvfreetypeface.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvfreetypefontman.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvgammacorrection.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvgammatbl.c
    ${CRENGINE_NG_SRC}/lvfont/lvbasefont.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvbitmapfont.cpp
    ${CRENGINE_NG_SRC}/lvfont/lvbitmapfontman.cpp
    ${CRENGINE_NG_SRC}/lvdrawbuf/lvbasedrawbuf.cpp
    ${CRENGINE_NG_SRC}/lvdrawbuf/lvgraydrawbuf.cpp
    ${CRENGINE_NG_SRC}/lvdrawbuf/lvcolordrawbuf.cpp
    ${CRENGINE_NG_SRC}/lvdrawbuf/lvinkmeasurementdrawbuf.cpp
    ${CRENGINE_NG_SRC}/lvdrawbuf/lvimagescaleddrawcallback.cpp
    ${CRENGINE_NG_SRC}/lvdrawbuf/lvdrawbuf_utils.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/lvtinydomutils.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomdatastoragemanager.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomtextstoragechunk.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/cachefile.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/cachefileheader.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/lvtinynodecollection.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomblobcache.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomnode.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/lvbase64nodestream.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/renderrectaccessor.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/lxmldocbase.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomxpointer.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomxpointerex.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/writenodeex.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/lvtocitem.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/lvpagemap.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomxrange.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomxrangelist.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldommarkedrange.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomdocument.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/lvimportstylesheetparser.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomdoccache.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomdoccacheimpl.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomelementwriter.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomdocumentwriter.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomdocumentwriterfilter.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomdocumentfragmentwriter.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/ldomwordexlist.cpp
    ${CRENGINE_NG_SRC}/lvtinydom/lstridmap.cpp
    ${CRENGINE_NG_SRC}/locale_data/fc-lang-data.c
    ${CRENGINE_NG_SRC}/locale_data/crlocaledata.cpp
)

# ── CoolReader JNI bridge ─────────────────────────────────────────────────────
set(JNI_SRC
    ${CR3_CPP_DIR}/jni/cr3engine.cpp
    ${CR3_CPP_DIR}/jni/cr3java.cpp
    ${CR3_CPP_DIR}/jni/docview.cpp
)

# ── CoffeeCatch crash handler (arm/x86 only) ──────────────────────────────────
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

# ── Bundled external thirdparty static libraries ─────────────────────────────
add_subdirectory(${CR3_CPP_DIR}/thirdparty_libs ${CMAKE_CURRENT_BINARY_DIR}/thirdparty_libs)

# ── Shared library ────────────────────────────────────────────────────────────
find_library(log-lib log)

add_library(cr3engine SHARED ${ALL_SRC})

target_link_libraries(cr3engine
    local_png
    local_jpeg
    local_freetype
    local_harfbuzz
    local_fribidi
    local_unibreak
    local_zstd
    local_utf8proc
    cre_chmlib
    cre_antiword
    cre_qimagescale
    cre_md4c-html
    ${log-lib}
    -lm -lz -ldl
)
