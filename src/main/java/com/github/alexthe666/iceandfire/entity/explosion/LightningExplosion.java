package com.github.alexthe666.iceandfire.entity.explosion;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.IceAndFireConfig;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.projectile.EntityDragonLightningCharge;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import com.github.alexthe666.iceandfire.enums.EnumParticle;
import com.github.alexthe666.iceandfire.integration.LycanitesCompat;
import com.github.alexthe666.iceandfire.core.ModBlocks;
import com.github.alexthe666.iceandfire.message.MessageParticleFX;
import com.github.alexthe666.iceandfire.util.ParticleHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LightningExplosion extends Explosion {
	private final boolean isSmoking;
	private final World worldObj;
	private final double explosionX;
	private final double explosionY;
	private final double explosionZ;
	private final Entity exploder;
	private final float explosionSize;
	private final List<BlockPos> affectedBlockPositions;
	private final Map<EntityPlayer, Vec3d> playerKnockbackMap;
	private final Vec3d position;
	private final boolean mobGriefing;

	public LightningExplosion(World world, Entity entity, double x, double y, double z, float size, boolean smoke) {
		super(world, entity, x, y, z, size, true, smoke);
		this.affectedBlockPositions = Lists.<BlockPos>newArrayList();
		this.playerKnockbackMap = Maps.<EntityPlayer, Vec3d>newHashMap();
		this.worldObj = world;
		this.exploder = entity;
		this.explosionSize = size;
		this.explosionX = x;
		this.explosionY = y;
		this.explosionZ = z;
		this.isSmoking = smoke;
		this.position = new Vec3d(explosionX, explosionY, explosionZ);
		this.mobGriefing = worldObj.getGameRules().getBoolean("mobGriefing");
	}

	/**
	 * Does the first part of the explosion (destroy blocks)
	 */
	@Override
	public void doExplosionA() {
		Set<BlockPos> set = Sets.<BlockPos>newHashSet();
		int i = 16;

		for (int j = 0; j < 16; ++j) {
			for (int k = 0; k < 16; ++k) {
				for (int l = 0; l < 16; ++l) {
					if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
						double d0 = j / 15.0F * 2.0F - 1.0F;
						double d1 = k / 3.0F * 2.0F - 1.0F;
						double d2 = l / 15.0F * 2.0F - 1.0F;
						double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
						d0 = d0 / d3;
						d1 = d1 / d3;
						d2 = d2 / d3;
						float f = this.explosionSize * (0.7F + this.worldObj.rand.nextFloat() * 0.6F);
						double d4 = this.explosionX;
						double d6 = this.explosionY;
						double d8 = this.explosionZ;

						for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
							BlockPos blockpos = new BlockPos(d4, d6, d8);
							IBlockState iblockstate = this.worldObj.getBlockState(blockpos);

							if (iblockstate.getMaterial() != Material.AIR) {
								float f2 = this.exploder != null ? this.exploder.getExplosionResistance(this, this.worldObj, blockpos, iblockstate) : iblockstate.getBlock().getExplosionResistance(worldObj, blockpos, (Entity) null, this);
								f -= (f2 + 0.3F) * 0.3F;
							}

							if (f > 0.0F && (this.exploder == null || this.exploder.canExplosionDestroyBlock(this, this.worldObj, blockpos, iblockstate, f)) && iblockstate.getBlock().canEntityDestroy(iblockstate, this.worldObj, blockpos, this.exploder)) {
								set.add(blockpos);
							}

							d4 += d0 * 0.30000001192092896D;
							d6 += d1 * 0.30000001192092896D;
							d8 += d2 * 0.30000001192092896D;
						}
					}
				}
			}
		}
		if(DragonUtils.canGrief(false)){
			this.affectedBlockPositions.addAll(set);
		}
		float f3 = this.explosionSize * 2.0F;
		int k1 = MathHelper.floor(this.explosionX - f3 - 1.0D);
		int l1 = MathHelper.floor(this.explosionX + f3 + 1.0D);
		int i2 = MathHelper.floor(this.explosionY - f3 - 1.0D);
		int i1 = MathHelper.floor(this.explosionY + f3 + 1.0D);
		int j2 = MathHelper.floor(this.explosionZ - f3 - 1.0D);
		int j1 = MathHelper.floor(this.explosionZ + f3 + 1.0D);
		List<Entity> list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this.exploder, new AxisAlignedBB(k1, i2, j2, l1, i1, j1));
		net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.worldObj, this, list, f3);
		Vec3d Vec3d = new Vec3d(this.explosionX, this.explosionY, this.explosionZ);

		for (Entity entity : list) {
			if (!(entity instanceof EntityDragonLightningCharge)) {
				if (!entity.isImmuneToExplosions() && !entity.isEntityEqual(exploder)) {
					double d12 = entity.getDistance(this.explosionX, this.explosionY, this.explosionZ) / f3;

					if (d12 <= 1.0D) {
						double d5 = entity.posX - this.explosionX;
						double d7 = entity.posY + entity.getEyeHeight() - this.explosionY;
						double d9 = entity.posZ - this.explosionZ;
						double d13 = MathHelper.sqrt(d5 * d5 + d7 * d7 + d9 * d9);
						double d14 = this.worldObj.getBlockDensity(Vec3d, entity.getEntityBoundingBox());
						double d10 = (1.0D - d12) * d14;
						if (d13 != 0.0D) {
							d5 = d5 / d13;
							d7 = d7 / d13;
							d9 = d9 / d13;
							if (exploder instanceof EntityDragonBase) {
								if (DragonUtils.hasSameOwner(entity, exploder)) {
									return;
								}
								if (DragonUtils.isOwner(entity, exploder)) {
									entity.attackEntityFrom(IceAndFire.dragonLightning, ((float) ((int) ((d10 * d10 + d10) / 2.0D * 7.0D * (double) f3 + 1.0D))) / 6);
								} else if (!entity.isEntityEqual(exploder)) {
									entity.attackEntityFrom(IceAndFire.dragonLightning, (float) ((int) ((d10 * d10 + d10) / 2.0D * 7.0D * (double) f3 + 1.0D)) / 3);
									if (entity instanceof EntityLivingBase) {
										if (IceAndFireConfig.DRAGON_SETTINGS.lightningDragonKnockback) {
											double xRatio = exploder.posX - entity.posX;
											double zRatio = exploder.posZ - entity.posZ;
											((EntityLivingBase) entity).knockBack(entity, 0.3F, xRatio, zRatio);
										}
										if (IceAndFireConfig.DRAGON_SETTINGS.lightningDragonParalysis) {
											LycanitesCompat.applyParalysis(entity, IceAndFireConfig.DRAGON_SETTINGS.lightningDragonParalysisTicks);
										}
									}
								}
							}
							if (entity.isDead && this.exploder instanceof EntityDragonBase) {
								((EntityDragonBase) this.exploder).attackDecision = true;
							}
						}
						double d11 = 1.0D;

						if (entity instanceof EntityLivingBase) {
							d11 = EnchantmentProtection.getBlastDamageReduction((EntityLivingBase) entity, d10);
						}
						entity.motionX += d5 * d11;
						entity.motionY += d7 * d11;
						entity.motionZ += d9 * d11;

						if (entity instanceof EntityPlayer && !((EntityPlayer) entity).capabilities.disableDamage) {
							this.playerKnockbackMap.put((EntityPlayer) entity, new Vec3d(d5 * d10, d7 * d10, d9 * d10));
						}
					}
				}
			}
		}
	}

	/**
	 * Does the second part of the explosion (sound, particles, drop spawn)
	 */
	@Override
	public void doExplosionB(boolean spawnParticles) {
		if (this.isSmoking) {
			List<MessageParticleFX.Particle> particles = new ArrayList<>();
			for (BlockPos blockpos : this.affectedBlockPositions) {
				IBlockState state = this.worldObj.getBlockState(blockpos);
				Block block = this.worldObj.getBlockState(blockpos).getBlock();

				if (spawnParticles && this.worldObj.rand.nextFloat() > 0.95F) {
					double d0 = blockpos.getX() + this.worldObj.rand.nextFloat();
					double d1 = blockpos.getY() + this.worldObj.rand.nextFloat();
					double d2 = blockpos.getZ() + this.worldObj.rand.nextFloat();
					double d3 = d0 - this.explosionX;
					double d4 = d1 - this.explosionY;
					double d5 = d2 - this.explosionZ;
					double d6 = MathHelper.sqrt(d3 * d3 + d4 * d4 + d5 * d5);
					d3 = d3 / d6;
					d4 = d4 / d6;
					d5 = d5 / d6;
					double d7 = 0.5D / (d6 / this.explosionSize + 0.1D);
					d7 = d7 * (this.worldObj.rand.nextFloat() * this.worldObj.rand.nextFloat() + 0.3F);
					d3 = d3 * d7;
					d4 = d4 * d7;
					d5 = d5 * d7;

					particles.add(MessageParticleFX.createParticle(d0, d1, d2, d3, d4, d5));
				}

				if (state.getMaterial() != Material.AIR && !state.getBlock().getTranslationKey().contains("grave") && DragonUtils.canDragonBreak(state.getBlock()) && mobGriefing) {
					if (block == Blocks.GRASS_PATH) {
						worldObj.setBlockState(blockpos, ModBlocks.crackledGrassPath.getDefaultState());
					} else if (block == Blocks.GRASS) {
						worldObj.setBlockState(blockpos, ModBlocks.crackledGrass.getDefaultState());
					} else if (block instanceof BlockGrass || block instanceof BlockDirt) {
						worldObj.setBlockState(blockpos, ModBlocks.crackledDirt.getDefaultState());
					} else if (block instanceof BlockLeaves || state.getMaterial() == Material.WATER) {
						worldObj.setBlockState(blockpos, Blocks.AIR.getDefaultState());
					} else if (block instanceof BlockGravel) {
						worldObj.setBlockState(blockpos, ModBlocks.crackledGravel.getDefaultState());
					} else if (state.getMaterial() == Material.WOOD) {
						worldObj.setBlockState(blockpos, ModBlocks.ash.getDefaultState());
					} else if (state.getMaterial() == Material.ROCK && (block != ModBlocks.crackledCobblestone && block != Blocks.COBBLESTONE && block != Blocks.MOSSY_COBBLESTONE && block != Blocks.COBBLESTONE_WALL)) {
						worldObj.setBlockState(blockpos, ModBlocks.crackledStone.getDefaultState());
					} else if (state.getMaterial() == Material.ROCK) {
						worldObj.setBlockState(blockpos, ModBlocks.crackledCobblestone.getDefaultState());
					}
				}
			}
			if (!particles.isEmpty()) {
				List<EnumParticle> types = new ArrayList<>();
				types.add(EnumParticle.SPARK);
				types.add(EnumParticle.SMOKE);
				IceAndFire.NETWORK_WRAPPER.sendToAllTracking(new MessageParticleFX(types, particles), this.exploder);
			}
		}
	}

	@Override
	public Map<EntityPlayer, Vec3d> getPlayerKnockbackMap() {
		return this.playerKnockbackMap;
	}

	/**
	 * Returns either the entity that placed the explosive block, the entity
	 * that caused the explosion or null.
	 */
	@Override
	public EntityLivingBase getExplosivePlacedBy() {
		return this.exploder == null ? null : (this.exploder instanceof EntityTNTPrimed ? ((EntityTNTPrimed) this.exploder).getTntPlacedBy() : (this.exploder instanceof EntityLivingBase ? (EntityLivingBase) this.exploder : null));
	}

	public void func_180342_d() {
		this.affectedBlockPositions.clear();
	}

	@Override
	public List<BlockPos> getAffectedBlockPositions() {
		return this.affectedBlockPositions;
	}

	@Override
	public Vec3d getPosition() {
		return this.position;
	}
}