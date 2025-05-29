sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'ai/orchestration/runtime/test/integration/FirstJourney',
		'ai/orchestration/runtime/test/integration/pages/TasksList',
		'ai/orchestration/runtime/test/integration/pages/TasksObjectPage',
		'ai/orchestration/runtime/test/integration/pages/BotInstancesObjectPage'
    ],
    function(JourneyRunner, opaJourney, TasksList, TasksObjectPage, BotInstancesObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('ai/orchestration/runtime') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheTasksList: TasksList,
					onTheTasksObjectPage: TasksObjectPage,
					onTheBotInstancesObjectPage: BotInstancesObjectPage
                }
            },
            opaJourney.run
        );
    }
);