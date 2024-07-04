# Litematica-Printer
![ezgif-4-f05ee71350](https://user-images.githubusercontent.com/35677394/194375066-0a636c33-a5fd-4cfc-abb1-e9c86c4cb21b.gif)

[Modrinth Link ](https://modrinth.com/mod/litematica-printer-easyplace-extension/versions)

## Redstone-Aware Fast Block Auto Placement

README：*English* | [中文](README_cn.md)

## Setup

This mod requires **Fabric with Litematica, Malilib, Fabric API. Install MultiMC and Fabric API**, for previous versions, use **MultiConnect**.

Now it can place directional blocks in vanilla server with "Fake Rotation", or faster with "accurateBlockPlacement" with carpet-extra on server.

## What's difference?
It uses accurateblockplacement if available, also advanced protocol if my fork of carpet extra is installed, without protocol it still places everything correctly(should).

Also it tries to avoid making 'failure', especially related to update orders, observer / BUD / piston / state,falling blocks, etc.

It also tries blocks with multiple states : composters, snow layers, pickles....

It has extra feature for removing fluids / clearing area / breaking bedrock (?).

## Install guide

Install fabric (well, use MultiMC), litematica, malilib, and put this into mods folder. then you can just use easyplace. 
Also, install Fabric API for mod compatibility with EssentialClient.

Bedrock breaking is highly dependent on your ping, so you should set up delay higher than ping. for example if your ping is 200ms, then set delay to upper than 0.2.
in singleplayer, ping is 0 so whatever.

## Caution

(WON'T FIX) paper, spigot or other types can have their own anti cheat to prevent 'not-looking' placement, which means you can't do anything with this. There's some option to nerf printing action at https://github.com/jensvh/litematica-printer/releases/tag/v1.0.4, but still we can't assure if it would work or not. Use this if you are sure about it.

# Issue tracker
Mainly 1.19 is supported. You can use Multiconnect to connect previous versions, which works properly.

**If you see ANY error related to wrong block placement / fake rotation failure (in vanilla or vanilla-like servers), please report it. If available, attach Litematic file together for further investigation.**


## Before sending DM, check if its my printer....

https://github.com/aleksilassila/litematica-printer

Of course, I can't help other's mod, so check if its correct printer.

## Settings
**Litematica Addition**:

`easyPlaceMode++` - Turns off Printer, but use Fake Rotation based easy place mode.

`verifierFindInventoryContents`    - Verifier will indicate blocks with items as 'wrong state' even if it has actually correct state. Useful for comparator-related stuff.

`printerOff` - Turns off Printer and use Normal easyplace.

`printerUseInventoryCache` - Uses Own inventory util that is little more stable for desync.


### Printer settings:
Now Settings have names with printer.

**Options that block other action being performed**:

`printerAllowInventoryOperations`  - Printer will match and input required stacks for hopper / dropper / chest / etc, mostly for filter setups.

  >`inventoryCloseScreenAfterDone`  - Printer will close screen when filling is complete / or can't fill screen.

  >`printerInventoryScreenWait`  - Printer will wait for this time(ms) after screen is open, to sync with server.

  >`printerInventoryOperationRetry` - Printer will retry clicking to fill slots for this amount : recommended - 3-20

  >`printerInventoryOperationAllowAllNamed` - Printer will allow other named items with same stack size, being used as filter items.

`printerBreakBlocks` - Printer will break ALL Extra or Wrong blocks within schematic. Can perform while placement, but not recommended.

> `printerBedrockBreaking` - Printer will break bedrock with HASTE 2, EFFICIENCY 5 , requires redstone torch, pistons.

> >`printerBedrockBreakingUseSlimeblock` - Printer will allow slime block placement, to find valid locations to break bedrock.

`printerFlippincactus` - Printer will flip blocks with cactus if enabled.

`printerClearFluids` - Printer will do Clearing actions , at default, Lava / Water (Cobblestone / Sponge).

  >`printerClearFluidsUseCobblestone` - Printer will use cobblestone for water, instead of sponge.

  >`printerClearSnowLayer` - Printer will use String to clear Snow layers.


**Main Features**:

`printerAccurateBlockPlacement` - Printer will use AccurateBlockPlacement Protocol, which is handled via carpet extra.

`printerFakeRotation` - Printer will use Fake Rotations to place blocks in wanted direction.

>`printerFakeRotationTicks` : Printer will wait for ticks(50ms per tick) after sending rotation packets.

>`printerFakeRotationLimitPerTicks` : If Printer waiting tick is set to 0, Printer will limit fake rotation per tick with this number.

>`disableSingleplayerPlacementHandling` : Printer will not modify placement direction based on fake rotation itself. recommended : false

**Debug**:

`ShowDebugMessages` - Printer will show debug messages, for reasons why block placement is failed /skipped /etc.

>`ShowDebugExtraMessages` - Printer will notify you about current block placements and fake rotations.


**Limiting**:

`easyPlaceModePrinterRangeX / Y / Z` - Printer will only place block expanded from starting raytrace pos.

`easyPlaceModePrinterMaxBlocks` - Max blocks / interactions that printer can perform per tick (or, easyplace actions)

`easyPlaceModeDelay` - Delay between printer actions - Recommended > 0.05 (50ms) to sync block states.

`easyPlaceModeHotbarOnly` - Printer will only use hotbar slots, which can be modified with fewer packets.

`printerSleepStackEmptied` - Printer will sleep when used stack is emptied, to sync / prevent some anticheats.

`easyPlaceModePrinterMaxItemChanges` - Printer will limit changing items actions, per cycle. **recommended value : 2**

`printerBreakIgnoresExtra` - Printer won't break extra-marked blocks. Applies to bedrock breaking and breaking.


**Redstone**:

`printerUsePumpkinpieForComposter` - Printer will use Pumpkin Pie to match composter levels.

`printerSmartRedstoneAvoid` - Printer will follow Observer / ETC Order when placing blocks. It will try to avoid observer updates.

`printerObserverAvoidAll` - Printer will avoid Observer being placed when its facing wrong stated-blocks, but will place Wall / etc for some cases.

`printerAvoidCheckOnlyPistons` - Printer will ignore Dispenser QC States.

`printerSuppressPushLimitPistons` - Printer will NOT place Pistons directly powered, but not extended : which means push limit is required.

`printerUseIceForWater` - Printer will place Ice where water source should be at.

`printerCheckWaterFirstForWaterlogged` - Printer will NOT place Waterlogged blocks, and wait until water is there.

`printerPlaceMinecart` - Printer will place Minecart for powered detector rails, It won't place it when TNT will be triggered.


**Deprecated**

`CarpetExtraFixedVersion` - Use Fake rotations instead, until general protocol is implemented.


## Support
If you have any issues with this mod **DO NOT** contact and bother masa with it. Please message me in discord, I am usually in Scicraft, TMA, Masa's discord, etc...

## Credits
Masa is the writer of the actual litematica mod and allowed all of this to be possible.

Andrews is the one who made the litematica printer implimentation, EatMyVenom converted it to mixin.

Jensvh first ported printer to 1.17.

AngelBottomless, continued work and completed AccurateBlockPlacement and Redstone Orders / Fake rotations.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.

## TODO List (most possible to least possible)

Enable Fake rotations at default, and buff options for faster placing
