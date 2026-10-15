package top.theillusivec4.curios.common;

import org.junit.Test;
import top.theillusivec4.curios.api.type.capability.ICurio;

import static org.junit.Assert.assertEquals;

public class CuriosInventoryDeathTest {
    @Test
    public void defaultRuleFollowsKeepInventoryAndMobLoot() {
        assertDisposition(ICurio.DropRule.DEFAULT, true, true, CuriosInventory.DeathDisposition.KEEP);
        assertDisposition(ICurio.DropRule.DEFAULT, true, false, CuriosInventory.DeathDisposition.KEEP);
        assertDisposition(ICurio.DropRule.DEFAULT, false, true, CuriosInventory.DeathDisposition.DROP);
        assertDisposition(ICurio.DropRule.DEFAULT, false, false, CuriosInventory.DeathDisposition.DESTROY);
    }

    @Test
    public void alwaysDropOverridesKeepInventoryButRespectsMobLoot() {
        assertDisposition(ICurio.DropRule.ALWAYS_DROP, true, true, CuriosInventory.DeathDisposition.DROP);
        assertDisposition(ICurio.DropRule.ALWAYS_DROP, false, true, CuriosInventory.DeathDisposition.DROP);
        assertDisposition(ICurio.DropRule.ALWAYS_DROP, true, false, CuriosInventory.DeathDisposition.DESTROY);
    }

    @Test
    public void alwaysKeepOverridesBothGameRules() {
        assertDisposition(ICurio.DropRule.ALWAYS_KEEP, false, false, CuriosInventory.DeathDisposition.KEEP);
        assertDisposition(ICurio.DropRule.ALWAYS_KEEP, false, true, CuriosInventory.DeathDisposition.KEEP);
    }

    @Test
    public void destroyNeverDrops() {
        assertDisposition(ICurio.DropRule.DESTROY, false, true, CuriosInventory.DeathDisposition.DESTROY);
        assertDisposition(ICurio.DropRule.DESTROY, true, true, CuriosInventory.DeathDisposition.DESTROY);
    }

    @Test
    public void nullRuleUsesDefault() {
        assertDisposition(null, true, true, CuriosInventory.DeathDisposition.KEEP);
        assertDisposition(null, false, true, CuriosInventory.DeathDisposition.DROP);
    }

    @Test
    public void cloneDefaultRuleFollowsKeepInventory() {
        assertCloneDisposition(ICurio.DropRule.DEFAULT, true, CuriosInventory.DeathDisposition.KEEP);
        assertCloneDisposition(ICurio.DropRule.DEFAULT, false, CuriosInventory.DeathDisposition.DROP);
        assertCloneDisposition(null, true, CuriosInventory.DeathDisposition.KEEP);
        assertCloneDisposition(null, false, CuriosInventory.DeathDisposition.DROP);
    }

    @Test
    public void cloneAlwaysDropAndAlwaysKeepOverrideKeepInventory() {
        assertCloneDisposition(ICurio.DropRule.ALWAYS_DROP, true, CuriosInventory.DeathDisposition.DROP);
        assertCloneDisposition(ICurio.DropRule.ALWAYS_DROP, false, CuriosInventory.DeathDisposition.DROP);
        assertCloneDisposition(ICurio.DropRule.ALWAYS_KEEP, true, CuriosInventory.DeathDisposition.KEEP);
        assertCloneDisposition(ICurio.DropRule.ALWAYS_KEEP, false, CuriosInventory.DeathDisposition.KEEP);
    }

    @Test
    public void cloneDestroyNeverKeepsOrDrops() {
        assertCloneDisposition(ICurio.DropRule.DESTROY, true, CuriosInventory.DeathDisposition.DESTROY);
        assertCloneDisposition(ICurio.DropRule.DESTROY, false, CuriosInventory.DeathDisposition.DESTROY);
    }

    private static void assertDisposition(ICurio.DropRule rule, boolean keepInventory, boolean mobLoot,
                                          CuriosInventory.DeathDisposition expected) {
        assertEquals(expected, CuriosInventory.getDeathDisposition(rule, keepInventory, mobLoot));
    }

    private static void assertCloneDisposition(ICurio.DropRule rule, boolean keepInventory,
                                               CuriosInventory.DeathDisposition expected) {
        assertEquals(expected, CuriosInventory.getCloneDisposition(rule, keepInventory));
    }
}
