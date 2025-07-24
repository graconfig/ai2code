sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'ragcdsviewfields/test/integration/FirstJourney',
		'ragcdsviewfields/test/integration/pages/CDSViewFilesList',
		'ragcdsviewfields/test/integration/pages/CDSViewFilesObjectPage'
    ],
    function(JourneyRunner, opaJourney, CDSViewFilesList, CDSViewFilesObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('ragcdsviewfields') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheCDSViewFilesList: CDSViewFilesList,
					onTheCDSViewFilesObjectPage: CDSViewFilesObjectPage
                }
            },
            opaJourney.run
        );
    }
);