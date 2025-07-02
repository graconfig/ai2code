sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'cdsviewfields/test/integration/FirstJourney',
		'cdsviewfields/test/integration/pages/CDSViewFilesList',
		'cdsviewfields/test/integration/pages/CDSViewFilesObjectPage'
    ],
    function(JourneyRunner, opaJourney, CDSViewFilesList, CDSViewFilesObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('cdsviewfields') + '/index.html'
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