global_admin = 'AngelBottomless';

__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec)->(
	print(global_admin, 'right clicked block');
	print(player, 'pos: '+pos(block));
	print(player, 'side: '+ face);
	print(player, ' facing: '+ (player~'facing'));
	print(player, 'hitvec : '+hitvec);
	print(player,  'original hitvec : '+(pos(block) + hitvec));
	print(player,'\n');
);

__on_player_places_block(player, item_tuple, hand, block)->(
	print(global_admin, 'placed block');
	if (block_state(block, 'facing') != null,
	print('block facing : '+block_state(block, 'facing')));
);
__on_player_uses_item(player, item_tuple, hand)->(
	print(global_admin, 'uses item');
);
__on_player_releases_item(player, item_tuple, hand)->(
	print(global_admin, 'releases item');
);


__on_player_finishes_using_item(player, item_tuple, hand)->(
	print(global_admin, 'finish using item');
);


__on_player_clicks_block(player, block, face)->(
	print(global_admin, 'clicks block');
);


__on_player_swings_hand(player, hand)->(
	print(global_admin, 'swing hand');
);


