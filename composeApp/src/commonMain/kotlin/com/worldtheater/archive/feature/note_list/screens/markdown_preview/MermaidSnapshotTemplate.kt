package com.worldtheater.archive.feature.note_list.screens.markdown_preview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal enum class MermaidBridgeMode {
    AndroidInterface,
    IosWebkitHandlers,
    JavascriptObject,
}

internal enum class MermaidScriptMode {
    ExternalUrl,
    InlineSource,
    PreloadedGlobal,
}

internal enum class MermaidSizingMode {
    CropBoundsToTargetWidth,
    IntrinsicClampToTargetWidth,
}

internal enum class MermaidThemeInitializationMode {
    Base,
    ParsedThemeOrDefault,
}

internal data class MermaidBridgeSpec(
    val mode: MermaidBridgeMode,
    val targetName: String,
    val resultName: String = "onRasterizedPng",
    val svgName: String = "onRasterizedSvg",
    val errorName: String = "onError",
    val heightName: String = "onHeightChanged",
)

internal data class MermaidScriptSpec(
    val mode: MermaidScriptMode,
    val source: String? = null,
)

internal data class MermaidPixelRatioSpec(
    val maxDevicePixelRatio: Double? = null,
    val maxRasterWidthPx: Int? = null,
    val maxRasterHeightPx: Int? = null,
    val maxRasterPixels: Int? = null,
)

internal data class MermaidTrimSpec(
    val enabled: Boolean,
    val paddingCssPx: Int = 0,
)

internal data class MermaidInitialRenderSpec(
    val source: String,
    val configJson: String,
    val widthPx: Int,
)

internal data class MermaidHtmlSpec(
    val script: MermaidScriptSpec,
    val bridge: MermaidBridgeSpec,
    val themeInitialization: MermaidThemeInitializationMode,
    val sizingMode: MermaidSizingMode,
    val rootCssWidthPx: Int? = null,
    val rootPaddingCss: String = "6px 0 8px 0",
    val fontFamilyCss: String = "system-ui, -apple-system, BlinkMacSystemFont, sans-serif",
    val cropPaddingPx: Int? = null,
    val useCropBoundsForDimensions: Boolean = false,
    val extraInlineStyleProperties: List<String> = emptyList(),
    val pixelRatio: MermaidPixelRatioSpec = MermaidPixelRatioSpec(),
    val trim: MermaidTrimSpec = MermaidTrimSpec(enabled = false),
    val notifyHeightChanges: Boolean = false,
    val renderErrorsInRoot: Boolean = true,
    val missingScriptMessage: String = "Missing Mermaid script",
    val initialRender: MermaidInitialRenderSpec? = null,
)

internal fun buildStandardMermaidSnapshotHtmlDocument(
    bridge: MermaidBridgeSpec,
    mermaidScript: String,
    pixelRatio: MermaidPixelRatioSpec = MermaidPixelRatioSpec(
        maxDevicePixelRatio = MermaidPreviewDefaults.MAX_DEVICE_PIXEL_RATIO,
        maxRasterWidthPx = MermaidPreviewDefaults.MAX_RASTER_WIDTH_PX,
        maxRasterHeightPx = MermaidPreviewDefaults.MAX_RASTER_HEIGHT_PX,
        maxRasterPixels = MermaidPreviewDefaults.MAX_RASTER_PIXELS
    ),
): String {
    val safeScript = mermaidScript.replace("</script", "<\\/script")
    return buildMermaidSnapshotHtmlDocument(
        MermaidHtmlSpec(
            script = MermaidScriptSpec(
                mode = MermaidScriptMode.InlineSource,
                source = safeScript
            ),
            bridge = bridge,
            themeInitialization = MermaidThemeInitializationMode.ParsedThemeOrDefault,
            sizingMode = MermaidSizingMode.IntrinsicClampToTargetWidth,
            rootPaddingCss = "2px 0 4px 0",
            extraInlineStyleProperties = listOf("transform", "transform-origin", "transform-box", "vector-effect"),
            pixelRatio = pixelRatio,
            trim = MermaidTrimSpec(enabled = true, paddingCssPx = 2),
            notifyHeightChanges = true,
            renderErrorsInRoot = true,
            missingScriptMessage = "Missing Mermaid script"
        )
    )
}

internal fun buildSharedMermaidThemeConfig(
    isDarkTheme: Boolean,
    background: String,
    surface: String,
    surfaceVariant: String,
    primary: String,
    secondary: String,
    onSurface: String,
    onSurfaceVariant: String,
    onPrimary: String,
    activationBkgColor: String,
    outline: String,
    themeName: String? = null,
): String {
    return buildJsonObject {
        put("darkMode", isDarkTheme)
        themeName?.let { put("theme", it) }
        put("pageBackground", background)
        put("textColor", onSurface)
        put("themeVariables", buildJsonObject {
            put("background", background)
            put("primaryColor", surfaceVariant)
            put("primaryTextColor", onSurface)
            put("primaryBorderColor", primary)
            put("secondaryColor", surface)
            put("secondaryTextColor", onSurface)
            put("secondaryBorderColor", secondary)
            put("tertiaryColor", surface)
            put("tertiaryTextColor", onSurface)
            put("tertiaryBorderColor", outline)
            put("lineColor", onSurfaceVariant)
            put("arrowheadColor", onSurfaceVariant)
            put("textColor", onSurface)
            put("mainBkg", surfaceVariant)
            put("nodeBorder", primary)
            put("clusterBkg", surface)
            put("clusterBorder", outline)
            put("defaultLinkColor", onSurfaceVariant)
            put("actorBkg", surfaceVariant)
            put("actorBorder", primary)
            put("actorTextColor", onSurface)
            put("labelBoxBkgColor", background)
            put("labelTextColor", onSurface)
            put("signalColor", onSurfaceVariant)
            put("signalTextColor", onSurface)
            put("activationBorderColor", primary)
            put("activationBkgColor", activationBkgColor)
            put("sequenceNumberColor", onPrimary)
        })
    }.toString()
}

internal fun Color.toCssHex(): String {
    val argb = toArgb()
    return "#${(0xFFFFFF and argb).toString(16).padStart(6, '0').uppercase()}"
}

internal fun quoteJavascriptString(value: String): String {
    val escaped = buildString(value.length + 16) {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '<' -> append("\\u003C")
                '>' -> append("\\u003E")
                '&' -> append("\\u0026")
                else -> {
                    if (char.code < 0x20) {
                        append("\\u")
                        append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        append(char)
                    }
                }
            }
        }
        append('"')
    }
    return escaped
}

private val MERMAID_SNAPSHOT_BASE_INLINE_STYLE_PROPERTIES = listOf(
    "fill",
    "stroke",
    "stroke-width",
    "stroke-dasharray",
    "stroke-dashoffset",
    "opacity",
    "fill-opacity",
    "stroke-opacity",
    "color",
    "font-size",
    "font-family",
    "font-weight",
    "font-style",
    "text-anchor",
    "dominant-baseline",
    "stop-color",
    "stop-opacity",
    "marker-start",
    "marker-mid",
    "marker-end",
)

private fun buildMermaidSnapshotGetSvgCropBoundsJs(cropPaddingPx: Int): String = """
function getSvgCropBounds(sourceSvg) {
  const viewBox = sourceSvg.viewBox && sourceSvg.viewBox.baseVal ? sourceSvg.viewBox.baseVal : null;
  const attrWidth = parseFloat(sourceSvg.getAttribute('width') || '');
  const attrHeight = parseFloat(sourceSvg.getAttribute('height') || '');

  let width = 0;
  let height = 0;
  let x = viewBox && isFinite(viewBox.x) ? viewBox.x : 0;
  let y = viewBox && isFinite(viewBox.y) ? viewBox.y : 0;

  if (viewBox && isFinite(viewBox.width) && viewBox.width > 0) {
    width = viewBox.width;
    height = Math.max(1, viewBox.height);
  } else if (isFinite(attrWidth) && attrWidth > 0) {
    width = attrWidth;
    height = isFinite(attrHeight) && attrHeight > 0 ? attrHeight : 0;
  }

  if ((!isFinite(height) || height <= 0) && width > 0) {
    const rect = sourceSvg.getBoundingClientRect();
    if (rect.width > 0 && rect.height > 0) {
      height = width * (rect.height / rect.width);
    }
  }

  try {
    const box = sourceSvg.getBBox();
    if (box && isFinite(box.width) && isFinite(box.height) && box.width > 0 && box.height > 0) {
      const padding = $cropPaddingPx;
      x = box.x - padding;
      y = box.y - padding;
      width = box.width + padding * 2;
      height = box.height + padding * 2;
    }
  } catch (_) {
    // Ignore getBBox failures on some engines.
  }

  return {
    x: x,
    y: y,
    width: Math.max(1, Math.ceil(width || 1)),
    height: Math.max(1, Math.ceil(height || 1))
  };
}
""".trimIndent()

private fun buildMermaidSnapshotSerializeSvgJs(
    extraInlineStyleProperties: List<String> = emptyList(),
    useCropBoundsForDimensions: Boolean,
): String {
    val styleProperties = (MERMAID_SNAPSHOT_BASE_INLINE_STYLE_PROPERTIES + extraInlineStyleProperties)
        .distinct()
        .joinToString(",\n    ", prefix = "[\n    ", postfix = "\n  ]") { "'$it'" }
    val dimensionBlock = if (useCropBoundsForDimensions) {
        """
  if (cropBounds) {
    cloneSvg.setAttribute('viewBox', [cropBounds.x, cropBounds.y, cropBounds.width, cropBounds.height].join(' '));
    cloneSvg.setAttribute('width', String(cropBounds.width));
    cloneSvg.setAttribute('height', String(cropBounds.height));
  } else {
    if (explicitWidth > 0) cloneSvg.setAttribute('width', String(explicitWidth));
    if (explicitHeight > 0) cloneSvg.setAttribute('height', String(explicitHeight));
  }
"""
    } else {
        """
  if (explicitWidth > 0) cloneSvg.setAttribute('width', String(explicitWidth));
  if (explicitHeight > 0) cloneSvg.setAttribute('height', String(explicitHeight));
  if (cropBounds && cropBounds.width > 0 && cropBounds.height > 0) {
    cloneSvg.setAttribute(
      'viewBox',
      [cropBounds.x, cropBounds.y, cropBounds.width, cropBounds.height].join(' ')
    );
  }
"""
    }
    return """
function serializeSvgWithInlineStyles(sourceSvg, explicitWidth, explicitHeight, cropBounds) {
  const cloneSvg = sourceSvg.cloneNode(true);
  const sourceNodes = [sourceSvg, ...sourceSvg.querySelectorAll('*')];
  const cloneNodes = [cloneSvg, ...cloneSvg.querySelectorAll('*')];
  const properties = $styleProperties;

  for (let i = 0; i < sourceNodes.length && i < cloneNodes.length; i++) {
    const computed = window.getComputedStyle(sourceNodes[i]);
    const target = cloneNodes[i];
    for (const property of properties) {
      const value = computed.getPropertyValue(property);
      if (value) {
        target.style.setProperty(property, value);
      }
    }
  }

$dimensionBlock
  cloneSvg.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
  cloneSvg.style.background = 'transparent';
  cloneSvg.style.display = 'block';
  return new XMLSerializer().serializeToString(cloneSvg);
}
""".trimIndent()
}

private val MERMAID_SNAPSHOT_NEXT_FRAME_JS: String = """
async function nextFrame() {
  await new Promise(resolve => {
    let settled = false;
    const finish = () => {
      if (settled) return;
      settled = true;
      resolve();
    };
    setTimeout(finish, 32);
    if (typeof requestAnimationFrame === 'function') {
      requestAnimationFrame(() => finish());
    }
  });
}
""".trimIndent()

private val MERMAID_SNAPSHOT_TRIM_TRANSPARENT_CANVAS_JS: String = """
function trimTransparentCanvas(sourceCanvas, pixelRatio, paddingCssPx) {
  const ctx = sourceCanvas.getContext('2d');
  if (!ctx) {
    return {
      dataUrl: sourceCanvas.toDataURL('image/png'),
      width: Math.max(1, Math.round(sourceCanvas.width / pixelRatio)),
      height: Math.max(1, Math.round(sourceCanvas.height / pixelRatio))
    };
  }

  const imageData = ctx.getImageData(0, 0, sourceCanvas.width, sourceCanvas.height);
  const data = imageData.data;
  const widthPx = sourceCanvas.width;
  const heightPx = sourceCanvas.height;
  let minX = widthPx;
  let minY = heightPx;
  let maxX = -1;
  let maxY = -1;

  for (let y = 0; y < heightPx; y++) {
    for (let x = 0; x < widthPx; x++) {
      const alpha = data[(y * widthPx + x) * 4 + 3];
      if (alpha > 8) {
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
      }
    }
  }

  if (maxX < minX || maxY < minY) {
    return {
      dataUrl: sourceCanvas.toDataURL('image/png'),
      width: Math.max(1, Math.round(sourceCanvas.width / pixelRatio)),
      height: Math.max(1, Math.round(sourceCanvas.height / pixelRatio))
    };
  }

  const paddingPx = Math.max(0, Math.round((paddingCssPx || 0) * pixelRatio));
  minX = Math.max(0, minX - paddingPx);
  minY = Math.max(0, minY - paddingPx);
  maxX = Math.min(widthPx - 1, maxX + paddingPx);
  maxY = Math.min(heightPx - 1, maxY + paddingPx);

  const trimmedWidthPx = Math.max(1, maxX - minX + 1);
  const trimmedHeightPx = Math.max(1, maxY - minY + 1);
  const trimmedCanvas = document.createElement('canvas');
  trimmedCanvas.width = trimmedWidthPx;
  trimmedCanvas.height = trimmedHeightPx;
  const trimmedCtx = trimmedCanvas.getContext('2d');
  if (!trimmedCtx) {
    return {
      dataUrl: sourceCanvas.toDataURL('image/png'),
      width: Math.max(1, Math.round(sourceCanvas.width / pixelRatio)),
      height: Math.max(1, Math.round(sourceCanvas.height / pixelRatio))
    };
  }

  trimmedCtx.drawImage(
    sourceCanvas,
    minX, minY, trimmedWidthPx, trimmedHeightPx,
    0, 0, trimmedWidthPx, trimmedHeightPx
  );

  return {
    dataUrl: trimmedCanvas.toDataURL('image/png'),
    width: Math.max(1, Math.round(trimmedWidthPx / pixelRatio)),
    height: Math.max(1, Math.round(trimmedHeightPx / pixelRatio)),
    trimmedBounds: {
      minX: minX,
      minY: minY,
      maxX: maxX,
      maxY: maxY,
      widthPx: trimmedWidthPx,
      heightPx: trimmedHeightPx
    }
  };
}
""".trimIndent()

private fun buildMermaidSnapshotRasterizeSvgJs(
    sizePreparationJs: String,
    widthExpressionJs: String,
    heightExpressionJs: String,
    pixelRatioExpressionJs: String,
    serializeArgumentsJs: String,
    resultExpressionJs: String,
): String = """
async function rasterizeSvgToPngDataUrl(sourceSvg, targetWidth) {
  await nextFrame();
  await nextFrame();
$sizePreparationJs
  const width = $widthExpressionJs;
  const height = $heightExpressionJs;
  const pixelRatio = $pixelRatioExpressionJs;
  const svgText = serializeSvgWithInlineStyles($serializeArgumentsJs);
  const encodedSvg = btoa(unescape(encodeURIComponent(svgText)));
  const dataUrl = 'data:image/svg+xml;base64,' + encodedSvg;

  const image = new Image();
  image.decoding = 'sync';
  const loaded = new Promise((resolve, reject) => {
    image.onload = () => resolve();
    image.onerror = () => reject(new Error('Failed to decode raster image from SVG data URL'));
  });
  image.src = dataUrl;
  await loaded;

  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.ceil(width * pixelRatio));
  canvas.height = Math.max(1, Math.ceil(height * pixelRatio));
  canvas.style.width = width + 'px';
  canvas.style.height = height + 'px';
  const context = canvas.getContext('2d');
  if (!context) {
    throw new Error('Canvas 2D context unavailable');
  }
  context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
  context.clearRect(0, 0, width, height);
  context.drawImage(image, 0, 0, width, height);
  return $resultExpressionJs;
}
""".trimIndent()

private fun buildMermaidRenderBodyJs(
    sourceExpressionJs: String,
    configExpressionJs: String,
    initializeThemeExpressionJs: String,
    beforeRenderJs: String = "",
    afterSvgInsertedJs: String = "",
    onMissingSvgJs: String = "throw new Error('Mermaid SVG missing');",
): String = """
const parsedConfig = $configExpressionJs;
const theme = parsedConfig.themeVariables || {};
const pageBackground = parsedConfig.pageBackground || theme.background || 'transparent';
document.documentElement.style.background = pageBackground;
document.body.style.background = pageBackground;
root.style.color = parsedConfig.textColor || '#666';
mermaid.initialize({
  startOnLoad: false,
  securityLevel: 'loose',
  theme: $initializeThemeExpressionJs,
  deterministicIds: true,
  darkMode: !!parsedConfig.darkMode,
  themeVariables: theme
});
$beforeRenderJs
const id = 'mermaid_' + Math.random().toString(36).slice(2);
const result = await mermaid.render(id, $sourceExpressionJs || '');
root.innerHTML = result.svg;
const svg = root.querySelector('svg');
if (!svg) {
  $onMissingSvgJs
}
$afterSvgInsertedJs
""".trimIndent()

private fun MermaidBridgeSpec.buildPostResultJs(): String = when (mode) {
    MermaidBridgeMode.IosWebkitHandlers -> """
function postResult(dataUrl, width, height) {
  try {
    window.webkit.messageHandlers.${resultName}.postMessage(
      dataUrl + '\u0000' + String(width) + '\u0000' + String(height)
    );
  } catch (_) {}
}
""".trimIndent()
    MermaidBridgeMode.AndroidInterface,
    MermaidBridgeMode.JavascriptObject -> """
function postResult(dataUrl, width, height) {
  const bridge = window.${targetName};
  if (!bridge || !bridge.${resultName}) return;
  bridge.${resultName}(dataUrl, width, height);
}
""".trimIndent()
}

private fun MermaidBridgeSpec.buildPostErrorJs(): String = when (mode) {
    MermaidBridgeMode.IosWebkitHandlers -> """
function postError(message) {
  try {
    window.webkit.messageHandlers.${errorName}.postMessage(String(message || 'Mermaid render failed'));
  } catch (_) {}
}
""".trimIndent()
    MermaidBridgeMode.AndroidInterface,
    MermaidBridgeMode.JavascriptObject -> """
function postError(message) {
  const bridge = window.${targetName};
  if (!bridge || !bridge.${errorName}) return;
  bridge.${errorName}(String(message || 'Mermaid render failed'));
}
""".trimIndent()
}

private fun MermaidBridgeSpec.buildPostSvgJs(): String = when (mode) {
    MermaidBridgeMode.IosWebkitHandlers -> """
function postSvg(svgText) {
  try {
    window.webkit.messageHandlers.${svgName}.postMessage(String(svgText || ''));
  } catch (_) {}
}
""".trimIndent()
    MermaidBridgeMode.AndroidInterface,
    MermaidBridgeMode.JavascriptObject -> """
function postSvg(svgText) {
  const bridge = window.${targetName};
  if (!bridge || !bridge.${svgName}) return;
  bridge.${svgName}(String(svgText || ''));
}
""".trimIndent()
}

private fun MermaidBridgeSpec.buildPostHeightJs(): String = when (mode) {
    MermaidBridgeMode.IosWebkitHandlers -> """
function postHeight(height) {
  try {
    window.webkit.messageHandlers.${heightName}.postMessage(Number(height || 0));
  } catch (_) {}
}
""".trimIndent()
    MermaidBridgeMode.AndroidInterface,
    MermaidBridgeMode.JavascriptObject -> """
function postHeight(height) {
  const bridge = window.${targetName};
  if (!bridge || !bridge.${heightName}) return;
  bridge.${heightName}(height);
}
""".trimIndent()
}

private fun MermaidScriptSpec.buildLoaderPreludeJs(): String {
    val safeSource = source.orEmpty()
    return when (mode) {
        MermaidScriptMode.ExternalUrl -> """
const MERMAID_SCRIPT_SOURCE = ${quoteJavascriptString(safeSource)};

function loadScript(url) {
  return new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = url;
    script.async = true;
    script.onload = () => resolve(true);
    script.onerror = () => reject(new Error('Failed to load: ' + url));
    document.head.appendChild(script);
  });
}

async function ensureMermaidLoaded() {
  if (window.mermaid) return true;
  try {
    await loadScript(MERMAID_SCRIPT_SOURCE);
    return !!window.mermaid;
  } catch (_) {
    return false;
  }
}
""".trimIndent()
        MermaidScriptMode.InlineSource -> """
$safeSource

async function ensureMermaidLoaded() {
  return !!window.mermaid;
}
""".trimIndent()
        MermaidScriptMode.PreloadedGlobal -> """
async function ensureMermaidLoaded() {
  return !!window.mermaid;
}
""".trimIndent()
    }
}

private fun MermaidPixelRatioSpec.buildExpression(widthVar: String, heightVar: String): String {
    val parts = buildList {
        add("1")
        if (maxDevicePixelRatio != null) {
            val formatted = if (maxDevicePixelRatio % 1.0 == 0.0) {
                maxDevicePixelRatio.toInt().toString()
            } else {
                maxDevicePixelRatio.toString()
            }
            add("window.devicePixelRatio || $formatted")
        } else {
            add("window.devicePixelRatio || 1")
        }
        maxRasterWidthPx?.let { add("$it.0 / $widthVar") }
        maxRasterHeightPx?.let { add("$it.0 / $heightVar") }
        maxRasterPixels?.let { add("Math.sqrt($it.0 / Math.max(1, $widthVar * $heightVar))") }
    }
    return when {
        parts.size <= 2 -> "Math.max(1, ${parts[1]})"
        else -> "Math.max(1, Math.min(${parts.drop(1).joinToString(", ")}))"
    }
}

private fun buildMermaidComputeDiagramHeightJs(): String = """
function computeDiagramHeight() {
  const root = document.getElementById('root');
  if (!root) return 0;

  const rootRectHeight = root.getBoundingClientRect().height || 0;
  const rootScrollHeight = root.scrollHeight || 0;
  let svgBoxHeight = 0;
  let svgBoxExtentHeight = 0;
  let svgEstimatedHeight = 0;
  let svgRenderedHeight = 0;

  const svg = root.querySelector('svg');
  if (svg) {
    const renderedWidth = svg.getBoundingClientRect().width || 0;
    const renderedHeight = svg.getBoundingClientRect().height || 0;
    const vb = svg.viewBox && svg.viewBox.baseVal ? svg.viewBox.baseVal : null;
    if (renderedHeight > 0) {
      svgRenderedHeight = renderedHeight;
    }

    try {
      const box = svg.getBBox();
      if (box && isFinite(box.height)) {
        const top = Math.min(0, box.y);
        const bottom = box.y + box.height;
        if (vb && vb.width > 0 && renderedWidth > 0) {
          const scale = renderedWidth / vb.width;
          svgBoxHeight = box.height * scale;
          svgBoxExtentHeight = (bottom - top) * scale;
        } else {
          svgBoxHeight = box.height;
          svgBoxExtentHeight = bottom - top;
        }
      }
    } catch (_) {
      // Ignore getBBox failures on some engines.
    }

    if (vb && vb.width > 0 && renderedWidth > 0) {
      svgEstimatedHeight = (vb.height * renderedWidth) / vb.width;
    } else {
      svgEstimatedHeight = svg.getBoundingClientRect().height || 0;
    }
  }

  return Math.ceil(Math.max(
    rootRectHeight,
    rootScrollHeight,
    svgBoxHeight,
    svgBoxExtentHeight,
    svgEstimatedHeight,
    svgRenderedHeight
  ));
}
""".trimIndent()

private val MERMAID_INTRINSIC_SIZE_JS: String = """
function getSvgIntrinsicSize(sourceSvg) {
  const viewBox = sourceSvg.viewBox && sourceSvg.viewBox.baseVal ? sourceSvg.viewBox.baseVal : null;
  const styleMaxWidth = parseFloat(sourceSvg.style.maxWidth || '');
  const attrWidth = parseFloat(sourceSvg.getAttribute('width') || '');
  const attrHeight = parseFloat(sourceSvg.getAttribute('height') || '');

  let width = 0;
  let height = 0;

  if (viewBox && isFinite(viewBox.width) && viewBox.width > 0) {
    width = viewBox.width;
    height = Math.max(1, viewBox.height);
  } else if (isFinite(attrWidth) && attrWidth > 0) {
    width = attrWidth;
    height = isFinite(attrHeight) && attrHeight > 0 ? attrHeight : 0;
  }

  if ((!isFinite(width) || width <= 0) && isFinite(styleMaxWidth) && styleMaxWidth > 0) {
    width = styleMaxWidth;
  }

  if ((!isFinite(height) || height <= 0) && width > 0) {
    const rect = sourceSvg.getBoundingClientRect();
    if (rect.width > 0 && rect.height > 0) {
      height = width * (rect.height / rect.width);
    }
  }

  return {
    width: Math.max(1, Math.ceil(width || 1)),
    height: Math.max(1, Math.ceil(height || 1))
  };
}
""".trimIndent()

private fun buildMermaidSnapshotRuntimeJs(spec: MermaidHtmlSpec): String {
    val needsCropBounds = spec.sizingMode == MermaidSizingMode.CropBoundsToTargetWidth || spec.useCropBoundsForDimensions
    val sizePreparationJs: String
    val widthExpressionJs: String
    val heightExpressionJs: String
    val serializeArgumentsJs: String
    when (spec.sizingMode) {
        MermaidSizingMode.CropBoundsToTargetWidth -> {
            sizePreparationJs = "  const cropBounds = getSvgCropBounds(sourceSvg);"
            widthExpressionJs = "Math.max(1, Math.ceil(targetWidth || cropBounds.width))"
            heightExpressionJs = "Math.max(1, Math.ceil(cropBounds.height * (width / cropBounds.width)))"
            serializeArgumentsJs = "sourceSvg, width, height, cropBounds"
        }
        MermaidSizingMode.IntrinsicClampToTargetWidth -> {
            sizePreparationJs = "  const intrinsic = getSvgIntrinsicSize(sourceSvg);"
            widthExpressionJs = "Math.max(1, Math.ceil(Math.min(targetWidth || intrinsic.width, intrinsic.width)))"
            heightExpressionJs = "Math.max(1, Math.ceil(intrinsic.height * (width / intrinsic.width)))"
            serializeArgumentsJs = "sourceSvg, width, height"
        }
    }
    val pixelRatioExpressionJs = spec.pixelRatio.buildExpression("width", "height")
    val resultExpressionJs = if (spec.trim.enabled) {
        """
(() => {
  const trimmed = trimTransparentCanvas(canvas, pixelRatio, ${spec.trim.paddingCssPx});
  ${if (spec.sizingMode == MermaidSizingMode.CropBoundsToTargetWidth) "trimmed.cropBounds = cropBounds;" else ""}
  return trimmed;
})()
""".trimIndent()
    } else {
        "{ width: width, height: height, dataUrl: canvas.toDataURL('image/png') }"
    }
    val initializeThemeExpressionJs = when (spec.themeInitialization) {
        MermaidThemeInitializationMode.Base -> "'base'"
        MermaidThemeInitializationMode.ParsedThemeOrDefault -> "parsedConfig.theme || 'default'"
    }
    val renderErrorJs = if (spec.renderErrorsInRoot) {
        """
function showRenderError(root, message) {
  root.innerHTML = '<pre>' + message + '</pre>';
  postError(message);
}
""".trimIndent()
    } else {
        """
function showRenderError(root, message) {
  postError(message);
}
""".trimIndent()
    }
    val notifyHeightJs = if (spec.notifyHeightChanges) {
        """
function notifyFinalHeight() {
  postHeight(computeDiagramHeight());
}
""".trimIndent()
    } else {
        """
function notifyFinalHeight() {}
""".trimIndent()
    }
    val initialRenderJs = spec.initialRender?.let { initialRender ->
        """
setTimeout(() => {
  window.renderMermaid(
    ${quoteJavascriptString(initialRender.source)},
    ${quoteJavascriptString(initialRender.configJson)},
    ${initialRender.widthPx}
  );
}, 0);
""".trimIndent()
    }.orEmpty()
    return """
${spec.script.buildLoaderPreludeJs()}

${spec.bridge.buildPostResultJs()}

${spec.bridge.buildPostSvgJs()}

${spec.bridge.buildPostErrorJs()}

${spec.bridge.buildPostHeightJs()}

const root = document.getElementById('root');
if (!root) {
  throw new Error('Mermaid preview root missing');
}

$renderErrorJs
$notifyHeightJs
${if (needsCropBounds) buildMermaidSnapshotGetSvgCropBoundsJs(spec.cropPaddingPx ?: 0) else ""}
$MERMAID_INTRINSIC_SIZE_JS
${buildMermaidSnapshotSerializeSvgJs(
    extraInlineStyleProperties = spec.extraInlineStyleProperties,
    useCropBoundsForDimensions = spec.useCropBoundsForDimensions
)}
$MERMAID_SNAPSHOT_NEXT_FRAME_JS
$MERMAID_SNAPSHOT_TRIM_TRANSPARENT_CANVAS_JS
${if (spec.notifyHeightChanges) buildMermaidComputeDiagramHeightJs() else ""}
${buildMermaidSnapshotRasterizeSvgJs(
    sizePreparationJs = sizePreparationJs,
    widthExpressionJs = widthExpressionJs,
    heightExpressionJs = heightExpressionJs,
    pixelRatioExpressionJs = pixelRatioExpressionJs,
    serializeArgumentsJs = serializeArgumentsJs,
    resultExpressionJs = resultExpressionJs
)}

window.renderMermaid = async function renderMermaid(source, configJson, targetWidth) {
  try {
    root.innerHTML = '';
    const loaded = await ensureMermaidLoaded();
    if (!loaded || !window.mermaid) {
      throw new Error(${quoteJavascriptString(spec.missingScriptMessage)});
    }
    ${buildMermaidRenderBodyJs(
        sourceExpressionJs = "source",
        configExpressionJs = "JSON.parse(configJson || '{}')",
        initializeThemeExpressionJs = initializeThemeExpressionJs,
        afterSvgInsertedJs = "notifyFinalHeight();"
    )}
    postSvg(serializeSvgWithInlineStyles(svg));
    const rasterized = await rasterizeSvgToPngDataUrl(svg, targetWidth);
    postResult(rasterized.dataUrl, rasterized.width, rasterized.height);
  } catch (error) {
    showRenderError(root, String(error && error.message ? error.message : error || 'Mermaid render failed'));
  }
};

$initialRenderJs
""".trimIndent()
}

internal fun buildMermaidSnapshotHtmlDocument(spec: MermaidHtmlSpec): String {
    val bodyScriptJs = buildMermaidSnapshotRuntimeJs(spec)
    val rootWidthCss = spec.rootCssWidthPx?.let { "width:${it}px;" } ?: "width:100%;"
    return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <style>
    html, body {
      margin: 0;
      padding: 0;
      background: transparent;
      overflow: hidden;
    }
    body {
      font-family: ${spec.fontFamilyCss};
    }
    #root {
      box-sizing: border-box;
      ${rootWidthCss}
      padding: ${spec.rootPaddingCss};
      margin: 0;
      background: transparent;
    }
    #root > svg {
      display: block;
      max-width: 100%;
      height: auto;
      background: transparent;
    }
    #root pre {
      margin: 0;
      white-space: pre-wrap;
      word-break: break-word;
      font: 12px/1.4 ${spec.fontFamilyCss};
      color: #b3261e;
    }
  </style>
</head>
<body>
  <div id="root"></div>
  <script>
$bodyScriptJs
  </script>
</body>
</html>
""".trimIndent()
}
