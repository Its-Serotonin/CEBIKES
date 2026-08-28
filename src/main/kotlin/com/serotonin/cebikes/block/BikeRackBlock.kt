package com.serotonin.cebikes.block

import com.mojang.serialization.MapCodec
import com.serotonin.cebikes.item.BikeItem
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.EnumProperty
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
        val PART = EnumProperty.of("part", BikeRackPart::class.java)

        private val FLOOR_SHAPE_MAIN = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 4.0, 15.0)
        private val FLOOR_SHAPE_EXT  = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 4.0, 15.0)

        private val WALL_SHAPE_FLUSH_NORTH = Block.createCuboidShape(1.0, 2.0, 12.0, 15.0, 14.0, 16.0)
        private val WALL_SHAPE_FLUSH_SOUTH = Block.createCuboidShape(1.0, 2.0, 0.0, 15.0, 14.0, 4.0)
        private val WALL_SHAPE_FLUSH_WEST  = Block.createCuboidShape(12.0, 2.0, 1.0, 16.0, 14.0, 15.0)
        private val WALL_SHAPE_FLUSH_EAST  = Block.createCuboidShape(0.0, 2.0, 1.0, 4.0, 14.0, 15.0)
    }

    init {
        defaultState = stateManager.defaultState
            .with(FACING, Direction.NORTH)
            .with(WALL_MOUNTED, false)
            .with(PART, BikeRackPart.MAIN)
    }

    override fun getCodec(): MapCodec<out BlockWithEntity> = CODEC

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(FACING, WALL_MOUNTED, PART)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        val side = ctx.side
        val wallMounted = side.axis.isHorizontal
        val facing = if (wallMounted) side.opposite else ctx.horizontalPlayerFacing
        if (!wallMounted) {
            val extPos = ctx.blockPos.offset(facing)
            if (!ctx.world.getBlockState(extPos).canReplace(ctx)) return null
        }

        return defaultState
            .with(FACING, facing)
            .with(WALL_MOUNTED, wallMounted)
            .with(PART, BikeRackPart.MAIN)
    }

    override fun onPlaced(
        world: World,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        itemStack: ItemStack
    ) {

        if (state.get(WALL_MOUNTED)) return
        if (world.isClient) return

        val facing = state.get(FACING)
        val extPos = pos.offset(facing)
        world.setBlockState(
            extPos,
            state.with(PART, BikeRackPart.EXTENSION),
            Block.NOTIFY_ALL
        )
    }

    override fun onStateReplaced(
        state: BlockState, world: World, pos: BlockPos,
        newState: BlockState, moved: Boolean
    ) {
        if (!state.isOf(newState.block)) {
            if (state.get(PART) == BikeRackPart.MAIN) {
                val be = world.getBlockEntity(pos) as? BikeRackBlockEntity
                if (be != null && be.hasBike()) {
                    val bikeStack = be.removeBike()
                    val centerPos = pos.toCenterPos()
                    world.spawnEntity(
                        net.minecraft.entity.ItemEntity(
                            world, centerPos.x, centerPos.y, centerPos.z, bikeStack
                        )
                    )
                }
            }

            val facing = state.get(FACING)
            val wallMounted = state.get(WALL_MOUNTED)
            if (!wallMounted) {
                val pairedPos = when (state.get(PART)) {
                    BikeRackPart.MAIN      -> pos.offset(facing)
                    BikeRackPart.EXTENSION -> pos.offset(facing.opposite)
                }
                val pairedState = world.getBlockState(pairedPos)
                if (pairedState.isOf(this)) {
                    world.removeBlock(pairedPos, false)
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

    override fun getOutlineShape(
        state: BlockState, world: BlockView, pos: BlockPos, context: ShapeContext
    ): VoxelShape {
        if (!state.get(WALL_MOUNTED)) {
            return if (state.get(PART) == BikeRackPart.MAIN) FLOOR_SHAPE_MAIN else FLOOR_SHAPE_EXT
        }
        return when (state.get(FACING)) {
            Direction.SOUTH -> WALL_SHAPE_FLUSH_NORTH
            Direction.NORTH -> WALL_SHAPE_FLUSH_SOUTH
            Direction.EAST  -> WALL_SHAPE_FLUSH_WEST
            Direction.WEST  -> WALL_SHAPE_FLUSH_EAST
            else -> VoxelShapes.fullCube()
        }
    }

    override fun onUse(
        state: BlockState, world: World, pos: BlockPos,
        player: PlayerEntity, hit: BlockHitResult
    ): ActionResult {
        val mainPos = when {
            state.get(WALL_MOUNTED) -> pos
            state.get(PART) == BikeRackPart.EXTENSION -> pos.offset(state.get(FACING).opposite)
            else -> pos
        }
        val be = world.getBlockEntity(mainPos) as? BikeRackBlockEntity ?: return ActionResult.PASS

        if (be.hasBike()) {
            if (!world.isClient) {
                val bikeStack = be.removeBike()
                if (!player.inventory.insertStack(bikeStack)) {
                    player.dropItem(bikeStack, false)
                }
            }
            return ActionResult.SUCCESS
        }

        val heldStack = player.getStackInHand(Hand.MAIN_HAND)
        if (heldStack.item is BikeItem) {
            if (!world.isClient) {
                be.storeBike(heldStack.copyWithCount(1))
                heldStack.decrement(1)
            }
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        if (state.get(PART) == BikeRackPart.MAIN) BikeRackBlockEntity(pos, state) else null

    override fun getRenderType(state: BlockState): BlockRenderType =
        if (state.get(PART) == BikeRackPart.MAIN) BlockRenderType.ENTITYBLOCK_ANIMATED
        else BlockRenderType.INVISIBLE
}