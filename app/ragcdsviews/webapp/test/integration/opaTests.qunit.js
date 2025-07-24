sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'ragcdsviews/test/integration/FirstJourney',
		'ragcdsviews/test/integration/pages/CDSViewsList',
		'ragcdsviews/test/integration/pages/CDSViewsObjectPage'
    ],
    function(JourneyRunner, opaJourney, CDSViewsList, CDSViewsObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('ragcdsviews') + '/index.html'
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