# Litematica-Printer
![ezgif-4-f05ee71350](https://user-images.githubusercontent.com/35677394/194375066-0a636c33-a5fd-4cfc-abb1-e9c86c4cb21b.gif)

[Modrinth Link ](https://modrinth.com/mod/litematica-printer-easyplace-extension/versions)

## Redstone-Aware Fast Block Auto Placement

描述：[English](README_cn.md) | *中文*

## 设置

此模组需要**带有** **Fabric with Litematica， Malilib， Fabric API。 安装 MultiMC 和 Fabric API**， 对于以前的版本，使用 **MultiConnect**。

现在，它可以使用“Fake Rotation”在原版服务器中放置定向的方块**（如：侦测器）**，或者使用“accurateBlockPlacement”在服务器上使用 carpet-extra 更快地放置定向的方块**（如：侦测器）**。

## 有什么区别？

如果可用，可以使用“accurateblockplacement”， 如果安装了我的fork的地毯扩展模组，它会使用高级协议，如果没有协议，它仍然可以正确放置所有内容（应该）。

此外，它还试图避免“失败”，特别是与更新状态， 侦测器 / BUD / 活塞 / 状态，下落方块，等有关。

它还尝试具有多种状态的块：堆肥桶、雪层、泡菜……

它具有去排流体/清除区域/破基岩（？）的额外功能。

## 安装指南

安装 fabric（好吧，使用 MultiMC）、litematica、malilib，并将其放入 mods 文件夹。然后，您可以使用EasyPlace。 此外，请安装 Fabric API 以实现 mod 与 EssentialClient 的兼容性。

破基岩高度依赖于您的 ping，因此您应该设置高于 ping 的延迟。例如，如果您的 ping 值为 200 毫秒，则将延迟设置为大于 0.2。 在单人游戏中，ping 为 0，所以随便吧。

## 警告

（不会修复）paper端， spigot端 ，或者其他有自己的反作弊的端，以防止“不看”而去放置，这意味着您对此无能为力。有一些选项可以削弱 https://github.com/jensvh/litematica-printer/releases/tag/v1.0.4 打印操作，但我们仍然无法确定它是否有效。如果您确定，请使用它。

# 问题跟踪

主要支持 1.19。您可以使用 Multiconnect 连接以前的版本，这可以正常工作。

**如果您看到任何与错误的方块放置/假旋转失败相关的错误 (在官方服务器或者类似的官方服务器中)， 请报告。如果可以，请将 Litematic 文件附在一起以供进一步调查。**

## 在发送 DM之前， 请检查它是否是我的打印机。

https://github.com/aleksilassila/litematica-printer

当然，我帮不上别人的模组，所以检查它的打印机是否正确。

## 设置

**Litematica 添加：**

`easyPlaceMode++` - 关闭打印机，但使用基于假旋转的简单放置模式。

`verifierFindInventoryContents`    - **验证程序**会将包含项目的块指示为**“错误状态”**，即使它实际上具有正确的状态。对于与**比较器**相关的东西很有用。

`printerOff` - 关闭打印机并使用 Normal easyplace。

`printerUseInventoryCache` - 使用自己的库存实用程序，该实用程序对于不同步来说更稳定。

### 打印机设置：

现在，“设置”具有带有打印机的名称。

**阻止正在执行的其他操作的选项：**

`printerAllowInventoryOperations`  - 打印机将匹配并输入漏斗/投掷器/箱子/等所需要填充物的，主要用于过滤器设置。

  >`inventoryCloseScreenAfterDone`  -  当填充完成/或无法填充屏幕时，打印机将关闭屏幕。

  >`printerInventoryScreenWait`  - 打印机将在屏幕打开后等待此时间（毫秒）与服务器同步。

  >`printerInventoryOperationRetry` - 打印机将重试单击以填充此数量的物品栏：推荐 - 3-20

  >`printerInventoryOperationAllowAllNamed` - 打印机将允许具有相同填充物大小的其他命名项目用作筛选项目。

`printerBreakBlocks` - 打印机将破坏原理图中所有额外或错误的方块。可以在放置时执行，但不建议这样做。

> `printerBedrockBreaking` - 打印机将用 **急迫2， 效率5** 来破基岩，需要红石火把、活塞。

> >`printerBedrockBreakingUseSlimeblock` - 打印机将允许放置粘液块，以找到破坏基岩的有效位置。

`printerFlippincactus` - 如果启用，打印机将用仙人掌翻转方块。

`printerClearFluids` - 打印机将执行清除操作，默认情况下，岩浆/水（圆石/海绵）。

  >`printerClearFluidsUseCobblestone` - 打印机将使用 **圆石** 代替 **海绵** 来排水。

  >`printerClearSnowLayer` - 打印机将使用 **光照** 清除雪。

**主要特点**：

`printerAccurateBlockPlacement` - 打印机将使用 AccurateBlockPlacement 协议，该协议通过 carpet extra 进行处理。

`printerFakeRotation` - 打印机将使用假旋转将方块放置在所需的方向上。

>`printerFakeRotationTicks` : 发送旋转数据包后，打印机将等待滴答声（每滴答50毫秒）。

>`printerFakeRotationLimitPerTicks` : 如果打印机等待刻度设置为 0，则打印机将使用此数字限制每次刻度的假旋转。

>`disableSingleplayerPlacementHandling` : 打印机不会根据假旋转本身修改放置方向。推荐关闭。

**调试**：

`ShowDebugMessages` - 打印机将显示调试消息，原因为方块放置失败/跳过/等。

>`ShowDebugExtraMessages` - 打印机将通知您当前块放置和假旋转。

**限制**：

`easyPlaceModePrinterRangeX / Y / Z` -  打印机将仅放置从启动光线追踪位置开始展开的块。

`easyPlaceModePrinterMaxBlocks` - 打印机每次滴答时可以执行的最大块/交互（或 easyplace 操作）

`easyPlaceModeDelay` - 打印机操作之间的延迟 - 建议> 0.05 （50ms） 同步块状态。

`easyPlaceModeHotbarOnly` - 打印机将仅使用快捷栏，可以使用更少的数据包进行修改。

`printerSleepStackEmptied` - 当使用的填充物被清空时，打印机将进入睡眠状态，以同步/防止一些反作弊。

`easyPlaceModePrinterMaxItemChanges` - 打印机将限制每个周期的更改项目操作。**推荐值 ： 2**

`printerBreakIgnoresExtra` - 打印机不会破坏额外标记的块。适用于基岩破碎和破碎。

**红石**：

`printerUsePumpkinpieForComposter` - 打印机将使用南瓜派来匹配堆肥等级。

`printerSmartRedstoneAvoid` - 打印机在放置块时将遵循侦测器/ETC顺序。它将尝试避免侦测器更新。

`printerObserverAvoidAll` - 打印机将避免在侦测器面临错误状态块时被放置，但在某些情况下会放置**墙壁**/等。

`printerAvoidCheckOnlyPistons` - 打印机将忽略分配器QC状态。

`printerSuppressPushLimitPistons` - 打印机不会直接放置活塞，但不会延长：这意味着需要推限制。

`printerUseIceForWater` - 打印机会将冰块放置在水源应有的位置。

`printerCheckWaterFirstForWaterlogged` - 打印机不会放置水，并等待在那里放置水。

`printerPlaceMinecart` - 打印机将 Minecart 放置在有**可被检测的动力铁轨**上，当会触发 TNT 时，它不会放置它。

**荒废的**

`CarpetExtraFixedVersion` - 改用假轮换，直到实现通用协议。

## 支持

如果您对此模组有任何问题，**请不要** 联系并打扰 masa。请在 discord 给我发消息， 我通常在 Scicraft， TMA， Masa's discord， 等地方。

## 关系

Masa 是 litematica mod 的作者，他让这一切成为可能。

Andrews 是实现 litematica 打印机的人， EatMyVenom 将其转换为 mixin。

Jensvh 首先将打印机移植到 1.17。

AngelBottomless， 继续工作并完成了 AccurateBlockPlacement 和 Redstone Orders / Fake rotations。

## 许可证

此模组在 CC0 许可下可用。随意从中学习并将其纳入您自己的项目中。

## 待办事项列表（从最可能到最不可能）

默认情况下启用假旋转，以及用于更快放置的增益选项
