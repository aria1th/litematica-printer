# Litematica-Printer

## Setup

To set this up just add the latest litematica version to your mods folder as well as this, it will replace easy place with printer. If carpet extra /quickcarpet enabled accurateblockplacement, you can turn on option, and... here goes the overpowered printer.
## What's difference?
It uses accurateblockplacement if available, also advanced protocol if my fork of carpet extra is installed, without protocol it still places everything correctly(should).
Also it tries to avoid making 'failure', especially related to update orders, observer / BUD / piston / state,falling blocks, etc.
It also tries blocks with multiple states : composters, snow layers, pickles....
It has extra feature for removing fluids / clearing area / breaking bedrock (?).

## Install guide

Install fabric (well, use MultiMC), litematica, malilib, and put this into mods folder. then you can just use easyplace. 
Bedrock breaking is highly dependent on your ping, so you should set up delay higher than ping. for example if your ping is 200ms, then set delay to upper than 0.2.
in singleplayer, ping is 0 so whatever.
## Caution

(WON'T FIX) paper, spigot or other types can have their own anti cheat to prevent 'not-looking' placement, which means you can't do anything with this. There's some option to nerf printing action at https://github.com/jensvh/litematica-printer/releases/tag/v1.0.4, but still we can't assure if it would work or not. Use this if you are sure about it.

# Issue tracker
Mainly 1.17-1.18 is supported, 1.16.5 is being supported only partially (without some recent updates) since I'm not really good at java.
Feel free to clean up my code or improve things, or use at some other mods, like separating functionalities. I'm not doing it because I don't know how to.

## Before sending DM, check if its my printer....
https://github.com/aleksilassila/litematica-printer
I don't really know how it works or produces bug, I just guess its something related to its rotation and face clicking. I can't really help anything not about my printer and its better to ask the author.

## Settings

### Printer settings:

`easyPlaceModeRange (x,y,z)`:&emsp;	"X,Y,Z Range for EasyPlace, Set to 000 if you want normal easyplace."<br/>
`easyPlaceModeMaxBlocks`:&emsp;		"Max block interactions per cycle"<br/>
`easyPlaceModeBreakBlocks`:&emsp;	"Printer will break blocks when its mismatch / extra"<br/>
`easyPlaceModeDelay`:&emsp;			"Delay between printing blocks.Do not set to 0 if you are playing on a server."<br/>
`easyPlaceModeHotbarOnly`:&emsp;	"Only place blocks from your hotbar. This bypasses some anti-cheats."<br/>
`easyPlaceModeFlippincactus`:&emsp;			"Allows Rotating incorrect blocks when carpet option is enabled and holding cactus on your mainhand"<br/>
`easyPlaceModeClearFluids`:&emsp;			"Remove nearby fluids, within easyplace range, to prevent errors. uses slime block to remove lava, sponges for water. WILL STOP OTHER ACTIONS"<br/>
`easyPlaceModeUseIceForWater`:&emsp;			"Printer places ice at waterlogged blocks positions"<br/>
`ClearFluidUseCobblestone`:&emsp;			"Use cobblestones instead of slime block to remove lava <br/> REQUIRES : easyPlaceModeClearFluids TRUE"<br/>
`ClearSnowLayer`:&emsp;			"It will place string where snow layer exists, ignores placement boxes, Only use if you need to. DEFAULT : FALSE"<br/>
`AccurateBlockPlacement`:&emsp;			"If carpet mod AccurateBlockPlacement is enabled (from extra or quickcarpet), you can turn on and printer will be rotation-free"<br/>
`easyPlaceModeUsePumpkinPie`:&emsp;			"If composter Level filling is needed, printer will use pumpkin pie to adjust its level"<br/>
`CarpetExtraFixedVersion`:&emsp;			"Carpet extra currently not supports some blocks, if its updated or server is using Quickcarpet, test this."<br/>
`easyPlaceModeSmartRedstoneAvoid`:&emsp;			"Pistons/Redstones /QC will be calculated and printer will place it in correct orders"<br/>
`easyPlaceModeObserverAvoidAll`:&emsp;			" Observer will avoid being placed when its watching state(not block) is not correct"<br/>
`BedrockBreaking`:&emsp;			"Removes bedrock when it needs to, will stop other actions, it needs BreakBlocks TRUE and haste 2, eff 5 pickaxe, works more well with accurateblockplacement.<br/> REQUIRES : easyPlaceModeBreakBlocks TRUE"<br/>
`BedrockBreakingUseSlimeBlocks`:&emsp;			"Places slime block to easily remove it"<br/>
### Handy litematica settings:

`easyPlaceMode`:&emsp;				"When enabled, then simply trying to use an item/place a block on schematic blocks will place that block in that position."<br/>
`easyPlaceModeHoldEnabled`:&emsp;	"When enabled, then simply holding down the use key and looking at different schematic blocks will place them"<br/>
`easyPlaceClickAdjacent`:&emsp;		"If enabled, then the Easy Place mode will try to click on existing adjacent blocks. This may help on Spigot or similar servers, which don't allow clicking on air blocks."<br/>
`pickBlockAuto`:&emsp;				"Automatically pick block before every placed block"<br/>
`pickBlockEnabled`:&emsp;			"Enables the schematic world pick block hotkeys. There is also a hotkey for toggling this option to toggle those hotkeys... o.o", "Pick Block Hotkeys"<br/>
`pickBlockIgnoreNBT`:&emsp;			"Ignores the NBT data on the expected vs. found items for pick block. Allows the pick block to work for example with renamed items."<br/>
`pickBlockableSlots`:&emsp;			"The hotbar slots that are allowed to be used for the schematic pick block. Can use comma separated individual slots and dash separated slot ranges (no spaces anywhere). Example: 2,4-6,9"<br/>
`placementInfrontOfPlayer`:&emsp;	"When enabled, created placements or moved placements are positioned so that they are fully infront of the player, instead of the placement's origin point being at the player's location"<br/>
`renderMaterialListInGuis`:&emsp;	"Whether or not the material list should be rendered inside GUIs"<br/>
`signTextPaste`:&emsp;				"Automatically set the text in the sign GUIs from the schematic"<br/>
<br/>
`easyPlaceActivation`:&emsp;		"When the easyPlaceMode is enabled, this key must be held to enable placing the blocks when using the vanilla Use key"<br/>
`easyPlaceToggle`:&emsp;			"Allows quickly toggling on/off the Easy Place mode"<br/>

## Support
If you have any issues with this mod **DO NOT** contact and bother masa with it. Please message me in discord, I am usually around Scicraft, Mechanists, and Hekate. 

## Credits
Masa is the writer of the actual litematica mod and allowed all of this to be possible.
Andrews is the one who made the litematica printer implimentation, I just converted it to mixin.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.

## TODO List (most possible to least possible)
Water placement - should be done with ice placement and request breaking.

Waterlogged block placement - will be placed when water source is there because it will automatically waterlog it

Fix block "face" rounding(rotation) issue, due to player rotation while using right click. 

Sorter item filling - not likely

