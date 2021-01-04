/*
 * Testerra
 *
 * (C) 2020, Mike Reiche, T-Systems Multimedia Solutions GmbH, Deutsche Telekom AG
 *
 * Deutsche Telekom AG and all other contributors /
 * copyright owners license this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import {autoinject} from "aurelia-framework";
import {StatusConverter} from "services/status-converter";
import {StatisticsGenerator} from "services/statistics-generator";
import {ExecutionStatistics} from "services/statistic-models";
import {AbstractViewModel} from "../abstract-view-model";
import {data} from "../../services/report-model";
import ResultStatusType = data.ResultStatusType;
import MethodType = data.MethodType;
import FailureCorridorValue = data.FailureCorridorValue;

class FailureCorridor {
    count:number = 0;
    limit:number = 0;
    get matched() {
        return this.count <= this.limit;
    }
}

@autoinject()
export class Dashboard extends AbstractViewModel {
    private _executionStatistics: ExecutionStatistics;
    private _selectedResult: any = {status: ""};
    private _failedRetried = 0;
    private _passedRetried = 0;
    private _majorFailures = 0;
    private _minorFailures = 0;
    private _highCorridor = new FailureCorridor();
    private _midCorridor = new FailureCorridor();
    private _lowCorridor = new FailureCorridor();

    constructor(
        private _statusConverter: StatusConverter,
        private _statisticsGenerator: StatisticsGenerator,
    ) {
        super();
    }


    attached() {
        this._statisticsGenerator.getExecutionStatistics().then(executionStatistics => {
            this._executionStatistics = executionStatistics;
            this._failedRetried = this._executionStatistics.getStatusCount(ResultStatusType.FAILED_RETRIED);
            this._passedRetried = this._executionStatistics.getStatusesCount([ResultStatusType.PASSED_RETRY,ResultStatusType.MINOR_RETRY]);

            this._executionStatistics.failureAspectStatistics.forEach(failureAspectStatistics => {
                if (failureAspectStatistics.isMinor) {
                    ++this._minorFailures;
                } else {
                    ++this._majorFailures
                }
            })

            /**
             * @todo Move this to {@link ExecutionStatistics}?
             */
            const executionAggregate = this._executionStatistics.executionAggregate;
            this._highCorridor.limit = executionAggregate.executionContext.failureCorridorLimits[FailureCorridorValue.FCV_MID];
            this._midCorridor.limit = executionAggregate.executionContext.failureCorridorLimits[FailureCorridorValue.FCV_MID];
            this._lowCorridor.limit = executionAggregate.executionContext.failureCorridorLimits[FailureCorridorValue.FCV_LOW];
            Object.values(executionAggregate.methodContexts)
                .filter(value => value.methodType==MethodType.TEST_METHOD)
                .forEach(value => {
                    if (this._statusConverter.failedStatuses.indexOf(value.resultStatus) >= 0) {
                        switch (value.failureCorridorValue) {
                            case data.FailureCorridorValue.FCV_HIGH: this._highCorridor.count++; break;
                            case data.FailureCorridorValue.FCV_MID: this._midCorridor.count++; break;
                            case data.FailureCorridorValue.FCV_LOW: this._lowCorridor.count++; break;
                        }
                    }
                });
        });
    };

    private _resultClicked(result) {
        this._selectedResult = {status: result};
    }

    private _pieceClicked(ev:CustomEvent) {
        this.queryParams = ev.detail;
    }

    private _gotoTests(params:any) {
        this.navInstruction.router.navigateToRoute("tests", params);
    }
}