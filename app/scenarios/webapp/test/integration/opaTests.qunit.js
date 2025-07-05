sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'scenarios/test/integration/FirstJourney',
		'scenarios/test/integration/pages/BusinessScenariosList',
		'scenarios/test/integration/pages/BusinessScenariosObjectPage'
    ],
    function(JourneyRunner, opaJourney, BusinessScenariosList, BusinessScenariosObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('scenarios') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheBusinessScenariosList: BusinessScenariosList,
					onTheBusinessScenariosObjectPage: BusinessScenariosObjectPage
                }
            },
            opaJourney.run
        );
    }
);