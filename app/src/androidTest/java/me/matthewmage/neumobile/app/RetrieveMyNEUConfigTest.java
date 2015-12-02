package me.matthewmage.neumobile.app;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Created by Matt on 12/2/2015.
 */
public class RetrieveMyNEUConfigTest extends ActivityInstrumentationTestCase2 {


    private MainActivity activity;

    /**
     * Creates an {@link ActivityInstrumentationTestCase2}.
     *
     * @param activityClass The activity to test. This must be a class in the instrumentation
     *                      targetPackage specified in the AndroidManifest.xml
     */
    public RetrieveMyNEUConfigTest(java.lang.Class activityClass) {
        super(activityClass);
    }

    public void testPreconditions() {
        assertNotNull("Activity is null",activity);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = (MainActivity) getActivity();
        testPreconditions();
    }

    public void testDoInBackground() throws Exception {

    }

    public void testOnPostExecute() throws Exception {

    }
}