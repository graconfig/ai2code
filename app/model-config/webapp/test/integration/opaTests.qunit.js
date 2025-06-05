sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'modelconfig/test/integration/FirstJourney',
		'modelconfig/test/integration/pages/ModelConfigsList',
		'modelconfig/test/integration/pages/ModelConfigsObjectPage'
    ],
    function(JourneyRunner, opaJourney, ModelConfigsList, ModelConfigsObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('modelconfig') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheModelConfigsList: ModelConfigsList,
					onTheModelConfigsObjectPage: ModelConfigsObjectPage
                }
            },
            opaJourney.run
        );
    }
);