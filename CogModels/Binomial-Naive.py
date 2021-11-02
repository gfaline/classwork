import numpy as np              # For arrays and random numbers
import matplotlib.pyplot as plt # For basic visualizations
import seaborn as sns           # For extended statistical visualizations

## We need a number of simulations that is high enough to get a robust 
## posterior. If our final graph looked choppy, it's a good indication 
## that we should choose a higher number
n_sims = 100_000

## Model
## There is a single underlying rate
## between 0 and 1 that is the probability of being left-handed

## Prior Parameters
## This is our belief about the underlying rate.
## We will call the rate `left_chance`
## We assume we know nothing about the value of the parameter
## So our prior is that it is uniformly distributed between 0 and 1

### Now we generate a sample of n_sims values that are distributed
### according to the uniform distribution. We use the numpy random.rand function, which
### gives us uniformly distributed random numbers between 0 and 1.
prior = np.random.rand(n_sims)
# Now prior is an array([0.76437137, 0.39767499, 0.48075969, 0.12071322, ...])


## Data
## We sampled 26 people, and there were 2 lefties
n_people = 26
lefties = 2

## Simulation
## Return the number of lefties in a simulated survey with the given
## lefty rate of left_chance
## The binomial function returns an array of repeated Bernoulli draws
## So each call to this function generates an entire survey
## And returns the number of left-handers in that simulation
def simulate_survey(left_chance, N):
    fake_survey = np.random.binomial(1, size=N, p=left_chance)
    # fake_survey will be an array, e.g. array([1, 0, 1, 0, 0, ...])
    # length fake_survey is N
    # chance of a lefty (represented as 1) = left_chance

    # because the lefties are 1 and the non-lefties are 0, we can just
    # sum the array for the total of lefties.
    return sum(fake_survey)

## Run Simulations
## For each prior that we have generated, we simulate a survey
## And put the number of lefties in that simulation into the
## array called `trials`
trials = np.empty(n_sims)
for i in range(n_sims):
    trials[i] = simulate_survey(prior[i], n_people)
    # prior[i] = real number in [0,1]
    # trials = array([8, 6, 6, 2, 19, ...])


## Finally, to condition over the data, we look into trials
## for the ones that matched out data (2 lefties). If it did,
## we add the `lefty_chance` from the prior that generated that
## simulation to the list of posterior values.
posterior = []
for (index, simulated_lefties) in enumerate(trials):
    # index = [0, 1, 2, 3, 4, ...]
    # simulated_lefties = array([19, 2, 24, ...])
    if simulated_lefties == lefties:
        posterior.append(prior[index])
        # prior = array([0.67374749, 0.11552653, 0.94340473, ...])
        # posterior = [0.11552653, ...]


## In this plot, the total height is the entire number of prior samples in each bin
## The orange portion is all the elements of the prior that we kept
## in the posterior, and the blue is all the elements that we rejected
## The x-axis is the value of left_chance, and the y-axis is the
## Number of samples in each bin
        
n_bins=100
data = [posterior, list(set(prior) - set(posterior))]
colors = ['orange', 'blue']
plt.hist(data, n_bins, histtype='bar', stacked=True,
         label=["Accepted", "Rejected"], color=colors)
plt.xlabel("left_chance parameter")
plt.ylabel("Number of Simulations")
plt.legend()
plt.title('Simulations kept and rejected')
plt.show()

        
## Plot the prior, posterior distributions,
## and the maximum likelihood estimate
## The prior and posterior are plotted as kernel density estimates
## Which smooths as well as normalizes the data to make the area under each
## Equal to 1
## cut=0 stops our plot at 0 and 1 so that we don't see values that
## cannot exist
sns.kdeplot(prior, cut=0)
sns.kdeplot(np.array(posterior), cut=0)
plt.vlines(2/26, ymin=0, ymax=7.5, colors=["Green"])
plt.xlabel("left_chance parameter")
plt.title("Posterior Distribution of Chance of Left-Handedness")
plt.legend(["Prior", "Posterior", "MLE"])
plt.show()


