sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'config/test/integration/FirstJourney',
		'config/test/integration/pages/TaskTypesList',
		'config/test/integration/pages/TaskTypesObjectPage',
		'config/test/integration/pages/BotTypesObjectPage'
    ],
    function(JourneyRunner, opaJourney, TaskTypesList, TaskTypesObjectPage, BotTypesObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('config') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheTaskTypesList: TaskTypesList,
					onTheTaskTypesObjectPage: TaskTypesObjectPage,
					onTheBotTypesObjectPage: BotTypesObjectPage
                }
            },
            opaJourney.run
        );
    }
);