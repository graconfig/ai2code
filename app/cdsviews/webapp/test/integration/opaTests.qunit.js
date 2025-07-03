sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'cdsviews/test/integration/FirstJourney',
		'cdsviews/test/integration/pages/CDSViewsList',
		'cdsviews/test/integration/pages/CDSViewsObjectPage'
    ],
    function(JourneyRunner, opaJourney, CDSViewsList, CDSViewsObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('cdsviews') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheCDSViewsList: CDSViewsList,
					onTheCDSViewsObjectPage: CDSViewsObjectPage
                }
            },
            opaJourney.run
        );
    }
);