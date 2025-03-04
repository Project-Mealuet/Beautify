package com.github.Pandarix.beautify.common.block;

import java.util.Arrays;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolAction;

public class HangingPot extends LanternBlock {
	private static final List<Item> validFlowers = Arrays.asList(Items.AIR, Items.ROSE_BUSH, Items.LILAC,
			Items.BLUE_ORCHID, Items.VINE, Items.SUNFLOWER, Items.PEONY, Items.AZURE_BLUET, Items.RED_TULIP,
			Items.ORANGE_TULIP, Items.WHITE_TULIP, Items.PINK_TULIP, Items.ALLIUM, Items.DANDELION, Items.POPPY,
			Items.GLOW_LICHEN, Items.OXEYE_DAISY, Items.LILY_OF_THE_VALLEY, Items.CORNFLOWER, Items.WEEPING_VINES,
			Items.TWISTING_VINES, Items.WITHER_ROSE, Items.GLOW_BERRIES, Items.SWEET_BERRIES, Items.GRASS, Items.FERN);

	// POTFLOWER indicates which index of the flowers List below is active
	public static final IntegerProperty POTFLOWER = IntegerProperty.create("potflower", 0, validFlowers.size() - 1);

	public static final BooleanProperty GROWN = BooleanProperty.create("grown");

	public HangingPot(Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(POTFLOWER, 0).setValue(GROWN, false));
	}

	// hitbox for the HangingPot
	private static final VoxelShape HANGING_SHAPE = Shapes.or(box(5, 0, 5, 11, 4, 11),
			box(3.75, 4, 3.75, 12.25, 8, 12.25), box(5, 8, 5, 11, 16, 11));
	private static final VoxelShape STANDING_SHAPE = Shapes.or(box(5, 0, 5, 11, 5, 11), box(4, 5, 4, 12, 8, 12));

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (state.getValue(HANGING)) {
			return HANGING_SHAPE;
		}
		return STANDING_SHAPE;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
			BlockPos blockPos, boolean bool) {
		if (state.getValue(GROWN)) { //if the plant is grown long
			if (blockPos.equals(pos.below())
					&& level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) { //if the neighbour is a model that clips into the pot
				level.setBlock(pos, state.setValue(GROWN, false), 3);
				ItemEntity Item = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
						new ItemStack(validFlowers.get(state.getValue(POTFLOWER))));
				level.addFreshEntity(Item);
				level.playSound(null, pos, SoundEvents.HANGING_ROOTS_BREAK, SoundSource.BLOCKS, 1, 1);
			}
		}
		super.neighborChanged(state, level, pos, block, blockPos, bool);
	}

	public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand,
			BlockHitResult pResult) {

		if (!pLevel.isClientSide() && pHand == InteractionHand.MAIN_HAND) {
			ItemStack playerStack = pPlayer.getItemInHand(pHand); // saving ItemStack

			// growing plant
			// block below must not be sturdy to prevent clipping models
			if (playerStack.getItem().equals(Items.BONE_MEAL) && pState.getValue(POTFLOWER) != 0
					&& !pLevel.getBlockState(pPos.below()).isFaceSturdy(pLevel, pPos.below(), Direction.UP)) {
				if (!pState.getValue(GROWN)) {
					pLevel.playSound(null, pPos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1, 1);
					if (!pPlayer.isCreative()) {
						playerStack.shrink(1);
					}
					pLevel.setBlock(pPos, pState.setValue(GROWN, true), 3);
					return InteractionResult.SUCCESS;
				}
			}

			// when plant is grown -> using shears cuts it down
			if (playerStack.canPerformAction(ToolAction.get("shears_harvest")) && pState.getValue(POTFLOWER) != 0
					&& pState.getValue(GROWN)) {
				pLevel.playSound(null, pPos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1, 1);
				pLevel.setBlock(pPos, pState.setValue(GROWN, false), 3);
				ItemEntity Item = new ItemEntity(pLevel, pPos.getX(), pPos.getY(), pPos.getZ(),
						new ItemStack(validFlowers.get(pState.getValue(POTFLOWER))));
				pLevel.addFreshEntity(Item);
				return InteractionResult.SUCCESS;
			}

			// if there is a flower
			if (pState.getValue(POTFLOWER) != 0) {
				// giving flower and clearing pot if hand empty
				if (playerStack.isEmpty()) {
					pPlayer.setItemInHand(pHand, new ItemStack(validFlowers.get(pState.getValue(POTFLOWER)),
							pState.getValue(GROWN) ? 2 : 1)); // giving 1 or 2 of plant (grown or not)
					pLevel.setBlock(pPos, pState.setValue(POTFLOWER, 0).setValue(GROWN, false), 3); // emptying the pot
					pLevel.playSound(null, pPos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1, 1);
					return InteractionResult.SUCCESS;
				} else if (playerStack.is(validFlowers.get(pState.getValue(POTFLOWER)))
						&& playerStack.getCount() < playerStack.getMaxStackSize()) {
					playerStack.grow(pState.getValue(GROWN) ? 2 : 1); // giving 1 or 2 of plant (grown or not)
					pLevel.setBlock(pPos, pState.setValue(POTFLOWER, 0).setValue(GROWN, false), 3);
					pLevel.playSound(null, pPos, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 1, 1);
					return InteractionResult.SUCCESS;
				}

				// else just return
				return InteractionResult.PASS;
			} else { // if there is no flower

				// checks if the flower in hand matches the available types
				for (Item flower : validFlowers) {
					if (playerStack.getItem().equals(flower)) {
						pLevel.setBlock(pPos, pState.setValue(POTFLOWER, validFlowers.indexOf(flower)), 3);
						if (!flower.equals(Items.AIR)) {
							pLevel.playSound(null, pPos, SoundEvents.AZALEA_PLACE, SoundSource.BLOCKS, 1, 1);
						}
						if (!pPlayer.isCreative()) {
							playerStack.shrink(1);
						}
						return InteractionResult.CONSUME_PARTIAL;
					}
				}
				// if the flower is not a valid one
				return InteractionResult.PASS;
			}
		}
		// end of statement
		return InteractionResult.PASS;
	}

	// creating Blockstates
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder);
		pBuilder.add(POTFLOWER, GROWN);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, BlockGetter getter, List<Component> component, TooltipFlag flag) {
		if (!Screen.hasShiftDown() && !Screen.hasControlDown()) {
			component.add(Component.literal("按住SHIFT以查看更多信息。").withStyle(ChatFormatting.YELLOW));
			component.add(Component.literal("按住CTRL来查看植物列表。").withStyle(ChatFormatting.YELLOW));
		}

		if (Screen.hasShiftDown()) {
			component.add(Component.literal("可挂在方块、绳索上，也可像往常一样放在地面上。")
					.withStyle(ChatFormatting.GRAY));
			component.add(Component.literal("使用植物右键点击，即可将其装入盆中。").withStyle(ChatFormatting.GRAY));
			component.add(Component.literal(
					"使用骨粉右键点击，加速植物生长。用剪子修剪植物，使其变短并获得植物道具。")
					.withStyle(ChatFormatting.GRAY));
		}

		if (Screen.hasControlDown()) {
			component.add(Component.literal("可装盆的植物：").withStyle(ChatFormatting.UNDERLINE)
					.withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.GRAY));
			component.add(Component.literal(
					"所有普通花卉 + 玫瑰丛、丁香、牡丹、向日葵、藤蔓、垂泪藤、缠怨藤、发光地衣、发光浆果、甜浆果、草、蕨")
					.withStyle(ChatFormatting.GRAY));
		}
		super.appendHoverText(stack, getter, component, flag);
	}
}