package com.example.c001apk.compose.ui.settings

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.c001apk.compose.BuildConfig
import com.example.c001apk.compose.FollowType
import com.example.c001apk.compose.R
import com.example.c001apk.compose.ThemeMode
import com.example.c001apk.compose.ThemeType
import com.example.c001apk.compose.constant.Constants.URL_SOURCE_CODE
import com.example.c001apk.compose.constant.Constants.URL_SOURCE_CODE_FORK
import com.example.c001apk.compose.logic.model.HapticStrength
import com.example.c001apk.compose.logic.providable.LocalUserPreferences
import com.example.c001apk.compose.ui.blacklist.BlackListType
import com.example.c001apk.compose.ui.component.HtmlText
import com.example.c001apk.compose.ui.component.MiuixAlertDialog as AlertDialog
import com.example.c001apk.compose.ui.component.MiuixDialogButton as TextButton
import com.example.c001apk.compose.ui.component.MiuixMenu as DropdownMenu
import com.example.c001apk.compose.ui.component.MiuixMenuItem as DropdownMenuItem
import com.example.c001apk.compose.ui.component.MiuixScaffold as Scaffold
import com.example.c001apk.compose.ui.component.MoreMenuButton
import com.example.c001apk.compose.ui.component.icons.EmojiEmotions
import com.example.c001apk.compose.ui.component.icons.Smartphone
import com.example.c001apk.compose.ui.component.icons.Swatch
import com.example.c001apk.compose.ui.component.rememberHapticClick
import com.example.c001apk.compose.ui.component.settings.BasicListItem
import com.example.c001apk.compose.ui.component.settings.DropdownListItem
import com.example.c001apk.compose.ui.component.settings.LocalPreferencePosition
import com.example.c001apk.compose.ui.component.settings.PreferencePosition
import com.example.c001apk.compose.ui.component.settings.SelectionItem
import com.example.c001apk.compose.ui.component.settings.StateDropdownListItem
import com.example.c001apk.compose.ui.component.settings.SwitchListItem
import com.example.c001apk.compose.util.CacheDataManager.clearAllCache
import com.example.c001apk.compose.util.CacheDataManager.getTotalCacheSize
import com.example.c001apk.compose.util.openInBrowser
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.util.Formatter
import top.yukonga.miuix.kmp.basic.Card as ElevatedCard
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField as OutlinedTextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.All
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.icon.extended.ScreenMirroring
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.icon.extended.WorldClock
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle as PaletteStyle
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** Created by bggRGjQaUbCoE on 2024/5/30 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onParamsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onViewBlackList: (String) -> Unit,
) {

    val prefs = LocalUserPreferences.current
    val scrollBehavior = MiuixScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current

    val imageQualityList by lazy { listOf("网络自适应", "原图", "普清") }
    val themeList by lazy { listOf("跟随系统", "总是开启", "总是关闭") }
    val followList by lazy { listOf("全部", "好友", "话题", "数码", "应用") }
    val hapticStrengthList by lazy { HapticStrength.options.map { it.label } }

    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSZLMIDDialog by remember { mutableStateOf(false) }
    var showFontScaleDialog by remember { mutableStateOf(false) }
    var showContentScaleDialog by remember { mutableStateOf(false) }
    var showImageQualityDialog by remember { mutableStateOf(false) }
    var showCleanCacheDialog by remember { mutableStateOf(false) }
    var showThemeTypeDialog by remember { mutableStateOf(false) }
    var showHapticStrengthDialog by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(getTotalCacheSize(context)) }
    val showHapticStrengthDialogClick = rememberHapticClick {
        showHapticStrengthDialog = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                color = com.example.c001apk.compose.ui.component.LocalBarColor.current,
                title = "设置",
                scrollBehavior = scrollBehavior,
                actions = {
                    Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
                        MoreMenuButton { dropdownMenuExpanded = true }

                        DropdownMenu(
                            expanded = dropdownMenuExpanded,
                            onDismissRequest = { dropdownMenuExpanded = false },
                        ) {
                            listOf(
                                    stringResource(id = R.string.menu_feedback),
                                    stringResource(id = R.string.menu_about),
                                )
                                .forEachIndexed { index, menu ->
                                    DropdownMenuItem(
                                        text = { Text(menu) },
                                        onClick = {
                                            dropdownMenuExpanded = false
                                            when (index) {
                                                0 ->
                                                    context.openInBrowser(
                                                        "$URL_SOURCE_CODE_FORK/issues",
                                                    )

                                                1 -> showAboutDialog = true
                                            }
                                        },
                                    )
                                }
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().scrollEndHaptic(),
            contentPadding =
                PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 24.dp + bottomPadding + paddingValues.calculateBottomPadding(),
                    start = paddingValues.calculateLeftPadding(layoutDirection),
                    end = paddingValues.calculateRightPadding(layoutDirection),
                ),
        ) {
            item(key = "settings_0") {
                BasicListItem(leadingText = stringResource(id = R.string.app_name))
            }
            item(key = "settings_1") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.First,
                ) {
                    BasicListItem(
                        leadingImageVector = MiuixIcons.Smartphone,
                        headlineText = "数字联盟ID",
                        supportingText = if (prefs.szlmId.isBlank()) "尚未生成，点击获取" else "已保存，点击管理",
                    ) {
                        showSZLMIDDialog = true
                    }
                }
            }
            item(key = "settings_2") {
                CompositionLocalProvider(LocalPreferencePosition provides PreferencePosition.Last) {
                    BasicListItem(
                        leadingImageVector = MiuixIcons.Smartphone,
                        headlineText = "机型参数",
                    ) {
                        onParamsClick()
                    }
                }
            }
            item(key = "settings_3") {
                BasicListItem(leadingText = "主题")
            }
            item(key = "settings_4") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.First,
                ) {
                    SwitchListItem(
                        value = prefs.materialYou,
                        leadingImageVector = MiuixIcons.Theme,
                        headlineText = "系统主题色",
                    ) {
                        viewModel.setMaterialYou(it)
                    }
                }
            }
            item(key = "settings_5") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    StateDropdownListItem(
                        isEnable = !prefs.materialYou,
                        leadingImageVector = MiuixIcons.Theme,
                        headlineText = "主题颜色",
                        value = prefs.themeType,
                        selections =
                            ThemeType.entries
                                .toMutableList()
                                .also { it.remove(ThemeType.UNRECOGNIZED) }
                                .map {
                                    SelectionItem(stringResource(id = themeTypeLabelRes(it)), it)
                                },
                        onValueChanged = { _, value ->
                            if (value == ThemeType.Custom) showThemeTypeDialog = true
                            viewModel.setThemeType(value)
                        },
                    )
                }
            }
            item(key = "settings_6") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    StateDropdownListItem(
                        isEnable = !prefs.materialYou,
                        leadingImageVector = MiuixIcons.Swatch,
                        headlineText = "调色版风格",
                        value = prefs.paletteStyle,
                        selections =
                            PaletteStyle.entries.mapIndexed { index, label ->
                                SelectionItem(
                                    stringResource(id = paletteStyleLabelRes(label)),
                                    index,
                                )
                            },
                        onValueChanged = { index, _ ->
                            viewModel.setPaletteStyle(index)
                        },
                    )
                }
            }
            item(key = "settings_7") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    DropdownListItem(
                        leadingImageVector = MiuixIcons.Theme,
                        headlineText = "深色主题",
                        value = prefs.themeMode.name,
                        selections =
                            (0..2).map {
                                SelectionItem(
                                    themeList[it],
                                    ThemeMode.entries[it].name,
                                )
                            },
                        onValueChanged = { index, _ ->
                            viewModel.setDarkTheme(ThemeMode.forNumber(index))
                        },
                    )
                }
            }
            item(key = "settings_8") {
                CompositionLocalProvider(LocalPreferencePosition provides PreferencePosition.Last) {
                    SwitchListItem(
                        value = prefs.pureBlack,
                        leadingImageVector = MiuixIcons.Theme,
                        headlineText = "纯黑主题",
                    ) {
                        viewModel.setPureBlack(it)
                    }
                }
            }
            item(key = "settings_9") {
                BasicListItem(leadingText = "显示")
            }
            item(key = "settings_10") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.First,
                ) {
                    BasicListItem(
                        leadingImageVector = MiuixIcons.Blocklist,
                        headlineText = "用户黑名单",
                    ) {
                        onViewBlackList(BlackListType.USER.name)
                    }
                }
            }
            item(key = "settings_11") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    BasicListItem(
                        leadingImageVector = MiuixIcons.Blocklist,
                        headlineText = "话题黑名单",
                    ) {
                        onViewBlackList(BlackListType.TOPIC.name)
                    }
                }
            }
            item(key = "settings_12") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    BasicListItem(
                        leadingImageVector = MiuixIcons.Rename,
                        headlineText = "字体大小",
                        supportingText = "${Formatter().format("%.2f", prefs.fontScale)}x",
                    ) {
                        showFontScaleDialog = true
                    }
                }
            }
            item(key = "settings_13") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    BasicListItem(
                        leadingImageVector = MiuixIcons.ScreenMirroring,
                        headlineText = "内容大小",
                        supportingText = "${Formatter().format("%.2f", prefs.contentScale)}x",
                    ) {
                        showContentScaleDialog = true
                    }
                }
            }
            item(key = "settings_14") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    DropdownListItem(
                        leadingImageVector = MiuixIcons.AddCircle,
                        headlineText = "关注分组",
                        value = prefs.followType.name,
                        selections =
                            followList.mapIndexed { index, label ->
                                SelectionItem(label, FollowType.entries[index].name)
                            },
                        onValueChanged = { index, _ ->
                            viewModel.setFollowType(FollowType.forNumber(index))
                        },
                    )
                }
            }
            item(key = "settings_15") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    DropdownListItem(
                        leadingImageVector = MiuixIcons.Image,
                        headlineText = "图片画质",
                        value = prefs.imageQuality,
                        selections =
                            imageQualityList.mapIndexed { index, label ->
                                SelectionItem(label, index)
                            },
                        onValueChanged = { index, _ ->
                            viewModel.setImageQuality(index)
                        },
                    )
                }
            }
            item(key = "settings_16") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    SwitchListItem(
                        value = prefs.imageFilter,
                        leadingImageVector = MiuixIcons.Photos,
                        headlineText = "压暗图片",
                    ) {
                        viewModel.setImageFilter(it)
                    }
                }
            }
            item(key = "settings_17") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    SwitchListItem(
                        value = prefs.openInBrowser,
                        leadingImageVector = MiuixIcons.WorldClock,
                        headlineText = "使用外部浏览器打开链接",
                    ) {
                        viewModel.setOpenInBrowser(it)
                    }
                }
            }
            item(key = "settings_18") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    SwitchListItem(
                        value = prefs.showSquare,
                        leadingImageVector = MiuixIcons.Notes,
                        headlineText = "头条显示广场",
                    ) {
                        viewModel.setShowSquare(it)
                    }
                }
            }
            item(key = "settings_19") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    SwitchListItem(
                        value = prefs.recordHistory,
                        leadingImageVector = MiuixIcons.Recent,
                        headlineText = "记录浏览历史",
                    ) {
                        viewModel.setRecordHistory(it)
                    }
                }
            }
            item(key = "settings_20") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    SwitchListItem(
                        value = prefs.showEmoji,
                        leadingImageVector = MiuixIcons.EmojiEmotions,
                        headlineText = "显示表情",
                    ) {
                        viewModel.setShowEmoji(it)
                    }
                }
            }
            item(key = "settings_21") {
                CompositionLocalProvider(LocalPreferencePosition provides PreferencePosition.Last) {
                    SwitchListItem(
                        value = prefs.checkUpdate,
                        leadingImageVector = MiuixIcons.Update,
                        headlineText = "检查更新",
                    ) {
                        viewModel.setCheckUpdate(it)
                    }
                }
            }
            item(key = "settings_22") {
                BasicListItem(leadingText = "其他")
            }
            item(key = "settings_23") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.First,
                ) {
                    SwitchListItem(
                        value = prefs.hapticFeedback,
                        leadingImageVector = MiuixIcons.SelectAll,
                        headlineText = "触感反馈",
                    ) {
                        viewModel.setHapticFeedback(it)
                    }
                }
            }
            item(key = "settings_24") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    SwitchListItem(
                        value = prefs.customHaptics,
                        leadingImageVector = MiuixIcons.SelectAll,
                        headlineText = "自定义触感",
                        supportingText = "关闭时跟随系统触感设置",
                    ) {
                        viewModel.setCustomHaptics(it)
                    }
                }
            }
            item(key = "settings_25") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    if (prefs.customHaptics) {
                        BasicListItem(
                            leadingImageVector = MiuixIcons.SelectAll,
                            headlineText = "触感强度",
                            supportingText = prefs.hapticStrength.label,
                            onClick = showHapticStrengthDialogClick,
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            SwitchListItem(
                                value = prefs.reverseHapticEffects,
                                leadingImageVector = MiuixIcons.SelectAll,
                                headlineText = "触感兼容模式",
                                supportingText = "轻重反馈与预期相反时开启",
                            ) {
                                viewModel.setReverseHapticEffects(it)
                            }
                        }
                    }
                }
            }
            item(key = "settings_26") {
                CompositionLocalProvider(
                    LocalPreferencePosition provides PreferencePosition.Middle,
                ) {
                    BasicListItem(
                        leadingImageVector = MiuixIcons.All,
                        headlineText = "关于",
                        supportingText = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                    ) {
                        onAboutClick()
                    }
                }
            }
            item(key = "settings_27") {
                CompositionLocalProvider(LocalPreferencePosition provides PreferencePosition.Last) {
                    BasicListItem(
                        leadingImageVector = MiuixIcons.Delete,
                        headlineText = "清理缓存",
                        supportingText = cacheSize,
                    ) {
                        cacheSize = getTotalCacheSize(context)
                        showCleanCacheDialog = true
                    }
                }
            }
        }

        when {
            showAboutDialog -> {
                AboutDialog {
                    showAboutDialog = false
                }
            }

            showImageQualityDialog -> {
                ListItemDialog(
                    title = "图片画质",
                    list = imageQualityList,
                    selected = prefs.imageQuality,
                    onDismiss = {
                        showImageQualityDialog = false
                    },
                    setData = {
                        showImageQualityDialog = false
                        viewModel.setImageQuality(it)
                    },
                )
            }

            showThemeTypeDialog -> {
                EditTextDialog(
                    hint = "6650A4",
                    maxLength = 6,
                    data = prefs.seedColor,
                    title = "自定义主题颜色",
                    onDismiss = {
                        showThemeTypeDialog = false
                    },
                    setData = {
                        viewModel.setSeedColor(it)
                    },
                )
            }

            showSZLMIDDialog -> {
                DigitalUnionDialog(
                    id = prefs.szlmId,
                    generating = viewModel.generatingId,
                    error = viewModel.idError,
                    onGenerate = viewModel::generateSZLMId,
                    onDismiss = { showSZLMIDDialog = false },
                    onSave = { viewModel.setSZLMId(it) },
                )
            }

            showFontScaleDialog -> {
                SliderDialog(
                    data = prefs.fontScale,
                    title = "字体大小",
                    hint = "比例",
                    onDismiss = {
                        showFontScaleDialog = false
                    },
                    setData = {
                        viewModel.setFontScale(it)
                    },
                )
            }

            showContentScaleDialog -> {
                SliderDialog(
                    data = prefs.contentScale,
                    title = "内容缩放",
                    hint = "比例",
                    onDismiss = {
                        showContentScaleDialog = false
                    },
                    setData = {
                        viewModel.setContentScale(it)
                    },
                )
            }

            showCleanCacheDialog -> {
                CleanCacheDialog(
                    cacheSize = cacheSize,
                    onDismiss = {
                        showCleanCacheDialog = false
                    },
                    onClean = {
                        clearAllCache(context)
                        cacheSize = "0 B"
                    },
                )
            }

            showHapticStrengthDialog -> {
                ListItemDialog(
                    title = "触感强度",
                    list = hapticStrengthList,
                    selected = HapticStrength.options.indexOf(prefs.hapticStrength),
                    onDismiss = {
                        showHapticStrengthDialog = false
                    },
                    hapticStrengthForIndex = { HapticStrength.options[it] },
                    setData = {
                        showHapticStrengthDialog = false
                        viewModel.setHapticStrength(HapticStrength.options[it])
                    },
                )
            }
        }
    }
}

@Composable
fun CleanCacheDialog(
    cacheSize: String,
    onDismiss: () -> Unit,
    onClean: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                },
            ) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onClean()
                },
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        text = {
            Text(text = "当前缓存大小: " + cacheSize)
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "清理缓存",
                textAlign = TextAlign.Center,
            )
        },
    )
}

@Composable
fun ListItemDialog(
    title: String,
    list: List<String>,
    selected: Int,
    onDismiss: () -> Unit,
    hapticStrengthForIndex: ((Int) -> HapticStrength)? = null,
    setData: (Int) -> Unit,
) {
    val optionClicks = list.mapIndexed { index, _ ->
        rememberHapticClick(strength = hapticStrengthForIndex?.invoke(index)) { setData(index) }
    }
    val onSelect by androidx.compose.runtime.rememberUpdatedState(optionClicks)
    val entry =
        remember(list, selected) {
            top.yukonga.miuix.kmp.basic.DropdownEntry(
                list.mapIndexed { index, label ->
                    top.yukonga.miuix.kmp.basic.DropdownItem(
                        text = label,
                        selected = selected == index,
                        onClick = { onSelect[index]() },
                    )
                },
            )
        }
    top.yukonga.miuix.kmp.popup.WindowDropdownDialog(
        entry = entry,
        title = title,
        dialogButtonString = "取消",
        show = true,
        onDismiss = onDismiss,
        onDismissFinished = onDismiss,
        dropdownColors = top.yukonga.miuix.kmp.basic.DropdownDefaults.dialogDropdownColors(),
    )
}

@Composable
fun ListItemDialogItem(
    title: String,
    selected: Boolean,
    hapticStrength: HapticStrength? = null,
    onClick: () -> Unit,
) {
    val hapticClick =
        rememberHapticClick(strength = hapticStrength) {
            onClick()
        }
    Row(
        modifier = Modifier.clickable { hapticClick() },
    ) {
        RadioButton(selected = selected, onClick = { hapticClick() })
        Text(
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterVertically),
            text = title,
        )
    }
}

@Composable
fun EditTextDialog(
    data: String,
    title: String,
    onDismiss: () -> Unit,
    setData: (String) -> Unit,
    hint: String? = null,
    maxLength: Int? = null,
) {
    var text by remember {
        mutableStateOf(
            TextFieldValue(
                text = data,
                selection = TextRange(data.length),
            ),
        )
    }
    val focusRequest = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            focusRequest.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                },
            ) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    setData(text.text)
                },
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequest),
                value = text,
                onValueChange = {
                    if (maxLength != null) {
                        if (it.text.matches(Regex("^[0-9a-fA-F]{0,6}$"))) text = it
                    } else {
                        text = it
                    }
                },
                label = hint.orEmpty(),
                useLabelAsPlaceholder = true,
            )
        },
    )
}

@Composable
fun SliderDialog(
    data: Float,
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    setData: (Float) -> Unit,
) {
    var progress by remember { mutableFloatStateOf(data) }
    AlertDialog(
        onDismissRequest = {
            onDismiss()
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    setData(1.0f)
                },
            ) {
                Text(text = stringResource(id = R.string.menu_reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    setData(progress)
                },
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
            ) {
                Slider(
                    value = progress,
                    onValueChange = {
                        progress = it
                    },
                    valueRange = 0.8f..1.3f,
                )
                Text(
                    text = "$hint: ${Formatter().format("%.2f", progress)}",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 15.sp * progress,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = { onDismiss() }) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            colors =
                CardDefaults.defaultColors()
                    .copy(color = MiuixTheme.colorScheme.surfaceContainerHigh),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            ) {
                val drawable =
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.mipmap.ic_launcher,
                        context.theme,
                    )
                Image(
                    painter = rememberDrawablePainter(drawable),
                    contentDescription = "icon",
                    modifier = Modifier.size(40.dp),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        stringResource(id = R.string.app_name),
                        style = MiuixTheme.textStyles.headline2,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                        style = MiuixTheme.textStyles.body2,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    HtmlText(
                        html =
                            stringResource(
                                id = R.string.about_source_code,
                                "<b><a href=\"${URL_SOURCE_CODE}\">GitHub</a></b>",
                            ),
                    )
                    HtmlText(
                        html =
                            stringResource(
                                id = R.string.about_source_code_fork,
                                "<b><a href=\"${URL_SOURCE_CODE_FORK}\">GitHub</a></b>",
                            ),
                    )
                }
            }
        }
    }
}

private fun themeTypeLabelRes(themeType: ThemeType): Int =
    when (themeType) {
        ThemeType.Default -> R.string.theme_type_default
        ThemeType.Red -> R.string.theme_type_red
        ThemeType.Pink -> R.string.theme_type_pink
        ThemeType.Purple -> R.string.theme_type_purple
        ThemeType.Indigo -> R.string.theme_type_indigo
        ThemeType.Blue -> R.string.theme_type_blue
        ThemeType.LightBlue -> R.string.theme_type_light_blue
        ThemeType.Cyan -> R.string.theme_type_cyan
        ThemeType.Teal -> R.string.theme_type_teal
        ThemeType.Green -> R.string.theme_type_green
        ThemeType.LightGreen -> R.string.theme_type_light_green
        ThemeType.Lime -> R.string.theme_type_lime
        ThemeType.Yellow -> R.string.theme_type_yellow
        ThemeType.Amber -> R.string.theme_type_amber
        ThemeType.Orange -> R.string.theme_type_orange
        ThemeType.DeepOrange -> R.string.theme_type_deep_orange
        ThemeType.Brown -> R.string.theme_type_brown
        ThemeType.BlueGrey -> R.string.theme_type_blue_grey
        ThemeType.Sakura -> R.string.theme_type_sakura
        ThemeType.Custom -> R.string.theme_type_custom
        ThemeType.UNRECOGNIZED -> R.string.theme_type_default
    }

private fun paletteStyleLabelRes(style: PaletteStyle): Int =
    when (style) {
        PaletteStyle.TonalSpot -> R.string.palette_style_tonal_spot
        PaletteStyle.Neutral -> R.string.palette_style_neutral
        PaletteStyle.Vibrant -> R.string.palette_style_vibrant
        PaletteStyle.Expressive -> R.string.palette_style_expressive
        PaletteStyle.Rainbow -> R.string.palette_style_rainbow
        PaletteStyle.FruitSalad -> R.string.palette_style_fruit_salad
        PaletteStyle.Monochrome -> R.string.palette_style_monochrome
        PaletteStyle.Fidelity -> R.string.palette_style_fidelity
        PaletteStyle.Content -> R.string.palette_style_content
    }
