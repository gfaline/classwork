import math
import csv
import matplotlib.pyplot as plt
# Below imports and related code curtsy of Chas' example code
# import pymc3 as pm  # Our main MCMC package
# import arviz as az  # For describing output of pymc3
# import seaborn as sns  # Extended statistical visualizations
import numpy as np  # For using arrays
import random


def probabilistic_bystander_model(n, temperature, altruism=.5):
    if n < 1:
        raise Exception("There cannot be fewer than one bystander.")
    if temperature < 10 or temperature > 150:
        return .001
    return min((altruism + temperature/100 + 1/n)/2, .999)
    # return .84
    # return altruism
    if altruism == 1:
        return .999
    if altruism >= .9 and temperature > 10:
        return .999
    if altruism < .1:
        return 0.001

    if n > 4:
        if altruism > 0.6:
            if 110 > temperature > 45:
                return higher_altruism(temperature)
        return 0.001
    if altruism > .3 and 110 > temperature > 32:
        return lower_altruism(temperature)
    return 0.001


def lower_altruism(temperature):
    if temperature < 32:
        return 0.001
    if 32 >= temperature >= 35:
        return 0.2
    if 45 > temperature >= 35:
        return 0.3
    if 55 > temperature >= 45:
        return 0.4
    if 60 > temperature >= 55:
        return 0.6
    if 65 > temperature >= 60:
        return 0.6
    if 70 > temperature >= 65:
        return 0.77
    if 75 > temperature >= 70:
        return 0.85
    if 80 > temperature >= 75:
        return 0.95
    if 85 > temperature >= 80:
        return 0.999
    if 90 > temperature >= 85:
        return 0.999
    if 95 > temperature >= 90:
        return 0.70
    if 100 > temperature >= 95:
        return 0.6
    if temperature >= 100:
        return 0.001
    return .001


def higher_altruism(temperature):
    if temperature < 45:
        return 0.001
    if 55 > temperature >= 45:
        return 0.1
    if 60 > temperature >= 55:
        return 0.3
    if 65 > temperature >= 60:
        return 0.4
    if 70 > temperature >= 65:
        return 0.6
    if 75 > temperature >= 70:
        return 0.75
    if 80 > temperature >= 75:
        return 0.9
    if 85 > temperature >= 80:
        return 0.7
    if 90 > temperature >= 85:
        return 0.6
    if 95 > temperature >= 90:
        return 0.5
    if 100 > temperature >= 95:
        return 0.1
    if temperature >= 100:
        return 0.001


def make_probability(total_data_size, saved):
    with pm.Model() as model:
        save_chance = pm.Beta("left_chance", alpha=1, beta=1)
        saved = pm.Binomial("saved", n=total_data_size, p=save_chance,
                            observed=saved)
        trace = pm.sample(10000, chains=4, return_inferencedata=True, cores=1)
        prior = pm.sampling.sample_prior_predictive(
            samples=10000, var_names=["saved"])
        posterior = pm.sample_posterior_predictive(
            trace, var_names=["saved"])
        print(az.summary(trace))
        sns.kdeplot(np.array(prior["saved"]), cut=0)
        sns.kdeplot(np.array(posterior["saved"]), cut=0)
        plt.vlines(2 / 26, ymin=0, ymax=7.5, colors=["Green"])
        plt.title("Chance of someone being saved")
        plt.legend(["Prior", "Posterior", "MLE"])
        plt.show()

    return posterior, model


def minus_log_likelihood(filename, altruism=.5):
    saved = 0
    sum = 0
    entries = 0
    f = open(filename, 'r')
    reader = csv.DictReader(f)
    for row in reader:
        entries += 1
        prediction = probabilistic_bystander_model(int(row['numberofbystanders']), int(row['ambienttemperature']),
                                                   altruism)
        outcome = int(row['saved'])
        # sum += math.log(abs(outcome-prediction), 10)

        if outcome == 1:
            saved += 1
        if outcome == 0:
            to_add = math.log((1 - prediction), 10)
        else:
            to_add = math.log((prediction), 10)
        # print(prediction, outcome, to_add)
        sum += to_add
    print("Saved: ", saved)
    print("NOt saved: ", (entries-saved))
    # posterior, model = make_probability(entries, saved)
    # sum = 0
    # # I dont know what to put here.
    # f.close()
    # f = open(filename, 'r')
    # reader = csv.DictReader(f)
    # for row in reader:
    #     prediction = probabilistic_bystander_model(int(row['numberofbystanders']), int(row['ambienttemperature']),
    #                                                altruism)
    #     outcome = int(row['saved'])
    #
    #     if outcome == 0:
    #         to_add = math.log((1 - prediction), 10)
    #     else:
    #         to_add = math.log((prediction), 10)
    #     print(prediction, outcome, to_add)
    #     sum += to_add

    sum *= -1
    return sum


def LL_as_func_A():
    A = []
    LL = []
    min_LL = 100000000
    min_index = None
    for i in range(1, 100):
        A.append(i / 100)
        minus_log = minus_log_likelihood("empirical.csv", i / 100)
        LL.append(minus_log)
        # The if statement below is for part I of D
        if minus_log < min_LL:
            min_LL = minus_log
            min_index = i/100
    print("The min value of LL from looping as per D-I is ", min_LL, " at altruism ", min_index)
    plt.plot(A, LL)

    #plt.plot(A, np.gradient(LL, .01))
    #plt.plot(A, [0]*len(A))
    print(np.where(np.gradient(LL, .01) == 0), np.min(np.gradient(LL, .01)))
    plt.show()


altruism = .3
error = minus_log_likelihood("group_10_supporting_data.csv", altruism=altruism)
print("supporting error: ", error)
error = minus_log_likelihood("group_10_falsifying_data.csv", altruism=altruism)
print("falsifying error: ", error)
error = minus_log_likelihood("empirical.csv", altruism=altruism)
print("data error: ", error)
LL_as_func_A()
