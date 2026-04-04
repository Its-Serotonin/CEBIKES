package com.serotonin.cebikes.block

import com.mojang.serialization.MapCodec
import com.serotonin.cebikes.item.BikeItem
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World

class BikeRackBlock(settings: Settings) : BlockWithEntity(settings) {

    companion object {
        val CODEC: MapCodec<BikeRackBlock> = createCodec(::BikeRackBlock)
        val FACING = Properties.HORIZONTAL_FACING
        val WALL_MOUNTED = BooleanProperty.of("wall_mounted")

        private val FLOOR_SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 4.0, 15.0)
        private val WALL_SHAPE_NORTH = Block.createCuboidShape(1.0, 2.0, 12.0, 15.0, 14.0, 16.0)
        private val WALL_SHAPE_SOUTH = Block.createCuboidShape(1.0, 2.0, 0.0, 15.0, 14.0, 4.0)
        private val WALL_SHAPE_WEST  = Block.createCuboidShape(12.0, 2.0, 1.0, 16.0, 14.0, 15.0)
        private val WALL_SHAPE_EAST  = Block.createCuboidShape(0.0, 2.0, 1.0, 4.0, 14.0, 15.0)
    }

    init {
        defaultState = stateManager.defaultState
            .with(FACING, Direction.NORTH)
            .with(WALL_MOUNTED, false)
    }

    override fun getCodec(): MapCodec<out BlockWithEntity> = CODEC

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(FACING, WALL_MOUNTED)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
        val side = ctx.side
        val wallMounted = side.axis.isHorizontal
        // Wall: facing = the clicked face direction (same convention as wall torches — points away from wall)
        // Floor: facing = direction player is looking
        val facing = if (wallMounted) side else ctx.horizontalPlayerFacing
        return defaultState.with(FACING, facing).with(WALL_MOUNTED, wallMounted)
    }

    override fun getOutlineShape(
        state: BlockState, world: BlockView, pos: BlockPos, context: ShapeContext
    ): VoxelShape {
        if (!state.get(WALL_MOUNTED)) return FLOOR_SHAPE
        return when (state.get(FACING)) {
            Direction.NORTH -> WALL_SHAPE_NORTH
            Direction.SOUTH -> WALL_SHAPE_SOUTH
            Direction.WEST  -> WALL_SHAPE_WEST
            Direction.EAST  -> WALL_SHAPE_EAST
            else -> VoxelShapes.fullCube()
        }
    }

    // ── Interaction ───────────────────────────────────────────────────────
    override fun onUse(
        state: BlockState, world: World, pos: BlockPos,
        player: PlayerEntity, hit: BlockHitResult
    ): ActionResult {
        val be = world.getBlockEntity(pos) as? BikeRackBlockEntity ?: return ActionResult.PASS
        val heldStack = player.getStackInHand(Hand.MAIN_HAND)

        if (be.hasBike()) {
            if (!world.isClient) {
                val bikeStack = be.removeBike()
                if (!player.inventory.insertStack(bikeStack)) {
                    player.dropItem(bikeStack, false)
                }
            }
            return ActionResult.SUCCESS
        }

        if (heldStack.item is BikeItem) {
            if (!world.isClient) {
                be.storeBike(heldStack.copyWithCount(1))
                heldStack.decrement(1)
            }
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    // ── BlockEntity plumbing ──────────────────────────────────────────────
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        BikeRackBlockEntity(pos, state)

    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL
}