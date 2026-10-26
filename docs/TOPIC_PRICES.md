# 话题排序与价格数据

协议依据：用户提供的 Reqable 2026-10-26 14:33 HAR，机型为荣耀 WIN RT2（5801）；另以验证包的当前设备参数对小米17 Ultra（5111）和手机摄影话题（13192）做了真实只读接口对照。HAR、Cookie、请求签名及设备标识不进入仓库，测试资源仅保留脱敏后的必要价格字段。

## 价格

沿用 `GET /v6/page/dataList`，不增加每条动态的详情请求：

- 到手价：内层 `/product/feedList?type=subTabFeed&id=…&subId=6`；动态 `tsubdata` 内的 `final_price`、`config_name`、`config_id`。
- 到手价统计：原始请求 `withSubTabFeedCard=1` 返回 `subTabFeedCard6`，其 `extraData` 包含 `final_price`、`update_time`、`feed_id`。显示为“到手价参考”，点击打开来源动态，不将其标成均价或保证最低价。
- 交易：内层 `/product/feedList?type=trade&id=…`；动态 `ershou_info.product_price` 与 `product_config_data.configTitle`，后者为空时不编造配置。交易价和规格同一行展示，规格过长时省略，不挤占最右侧图标。

嵌套 JSON 在样本中同时出现对象、编码字符串、空字符串和数字/字符串金额。模型按字段使用容错适配器；缺失或不合法的可选价格信息只隐藏价格，不中断动态列表。仅显示正数金额，零、负数、非法值不解释为免费或面议。

交易动态的 `ershou_info.link_url` 是对应商品分享链接，`link_source` 样本为“闲鱼”，`link_app_url` 为空。价格行最右侧显示闲鱼图标：简略卡片为不可点击的 24 dp 图标（朗读标签“含闲鱼链接”），动态详情为可点击的 32 dp 图标（40 dp 点击区域，朗读标签“查看闲鱼”），不另占一行；保留完整原链接及参数，点击后交给系统打开。其他来源使用通用链接图标，分别朗读“含交易链接”和“查看交易链接”。没有合法 HTTP(S) 链接时隐藏标记与入口；价格缺失时仍保留靠右的链接图标，不编造价格。不从动态 ID 猜测闲鱼商品 ID，不自动发起购买。

列表和动态详情共用 `FeedMessage` 中的价格组件。统计卡保留在列表内随内容滚动，分类切换后仍保留其请求标记。

有交易链接的二手详情在价格和配置下方用圆角边框包住整条提示及关闭入口，提示“点击图标即可跳转二手平台”。点击“不再显示”后，经页面 ViewModel、偏好仓库写入 DataStore 的 `hideTradeLinkHint`（proto 字段 57），对之后所有二手详情生效，重启后保持；新安装和旧设置文件默认显示。简略卡片不显示该提示。

## 排序

| 内层接口 / 类型 | 参数 |
| --- | --- |
| `/product/feedList` 的 feed、subTabFeed、article、answer、video、trade | 最新 `listType=dateline_desc`；热度 `listType=rank_score` |
| 同接口点评 | 机主 `type=rating&isOwner=1`；最新/热度不保留 isOwner；好评 `type=ratingByScore`，差评 `type=ratingByScoreAsc` |
| `/topic/tagFeedList` | 最新 `sortField=dateline_desc`；热度 `sortField=rank_score` |
| `/feed/multiTagFeedList` | 最新 `listType=dateline_desc`；热度 `listType=popular` |

解析 `/page?url=` 内层编码，保留 id、type、subId、tag、统计卡等条件。每个 Tab 独立保存排序，参数/标签目录等非动态页不显示排序；普通默认选项直接恢复原始 URL。最新/热度不附带置顶内容。

不要给图文、问答、视频、交易强加 `withSortCard=1` 再直接套返回菜单：实测这时服务端生成的菜单 URL 固定为 type=feed，会丢失原分类。

单元测试覆盖捕获字段形态、缺失/非法字段、金额格式、编码嵌套和分类保留；设备测试覆盖金额/配置布局、来源点击、点评选项交互。完整编译和真机执行结果在交付验证记录中单独列出。

闲鱼图标使用闲鱼官网公开 favicon 原文件，未重绘或改色：https://img.alicdn.com/tfs/TB19WObTNv1gK0jSZFFXXb0sXXa-144-144.png （官网 https://www.goofish.com 的 link rel=Shortcut Icon）。仅用于标识外部商品来源。
